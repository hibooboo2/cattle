package io.cattle.platform.iaas.api.auth.providers.ldap;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AuthUtils;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integrations.github.constants.GithubConstants;
import io.cattle.platform.iaas.api.auth.integrations.github.resource.Token;
import io.cattle.platform.iaas.api.auth.interfaces.TokenHandler;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.netflix.config.DynamicLongProperty;

public class LdapTokenHandler implements TokenHandler {

    private static final Log logger = LogFactory.getLog(LdapTokenHandler.class);
    private static final DynamicLongProperty TOKEN_EXPIRY_MILLIS = ArchaiusUtil.getLong("api.auth.jwt.token.expiry");
    @Inject
    LdapClient ldapClient;
    @Inject
    AuthDao authDao;
    @Inject
    TokenService tokenService;
    @Inject
    ProjectResourceManager projectResourceManager;
    @Inject
    ObjectManager objectManager;

    @Inject
    LdapUtils ldapUtils;

    public Token getLdapToken(String username, String password) throws IOException {
        if (!ldapClient.isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, LdapConstants.LDAPCONFIG, "Ldap Not Configured.", null);
        }
        Account account;
        Set<ExternalId> externalIds = ldapClient.getExternalIds(username, password);
        String fullUserName = ldapClient.getUserExternalId(username);
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(externalIds, false, null);
        if (SecurityConstants.SECURITY.get()) {
            ldapUtils.isAllowed(externalIds);
            account = authDao.getAccountByExternalId(fullUserName, LdapConstants.USER_SCOPE);
            if (null == account) {
                account = authDao.createAccount(username, AccountConstants.USER_KIND, fullUserName,
                        LdapConstants.USER_SCOPE);
                if (!hasAccessToAProject) {
                    projectResourceManager.createProjectForUser(account);
                }
            }
        } else {
            account = authDao.getAdminAccount();
            authDao.updateAccount(account, null, AccountConstants.ADMIN_KIND, fullUserName, LdapConstants.USER_SCOPE);
            authDao.ensureAllProjectsHaveNonRancherIdMembers(new LdapUser(fullUserName, username));
        }
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put(AuthUtils.TOKEN, LdapConstants.LDAP_JWT);
        jsonData.put(AuthUtils.ACCOUNT_ID, fullUserName);
        jsonData.put(LdapConstants.USERNAME, username);
        List<String> externalIdsList = new ArrayList<>();
        for (ExternalId externalId : externalIds) {
            externalIdsList.add(externalId.getId());
        }
        jsonData.put(LdapConstants.LDAP_GROUPS, externalIdsList);
        DataAccessor.fields(account).withKey(LdapConstants.LDAP_ACCESS_TOKEN).set(username + ':' + password);
        objectManager.persist(account);
        account = objectManager.reload(account);
        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS.get());
        String jwt = tokenService.generateEncryptedToken(jsonData, expiry);
        return new Token(jwt, fullUserName, null, null, null, null,
                account.getKind(), accountId);
    }

    @Override
    public Token getToken(ApiRequest request) throws IOException {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(GithubConstants.GITHUB_REQUEST_CODE));
        String[] split = code.split(":");
        if (split.length != 2) {
            logger.error("Code is not 2 long. Meaning it is not username:pass : " + code);
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        return getLdapToken(split[0], split[1]);
    }
}
