package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.object.ObjectManager;
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

public class LdapTokenCreator implements TokenCreator {

    private static final Log logger = LogFactory.getLog(LdapTokenCreator.class);
    private static final DynamicLongProperty TOKEN_EXPIRY_MILLIS = ArchaiusUtil.getLong("api.auth.jwt.token.expiry");
    @Inject
    LdapIdentitySearchProvider ldapIdentitySearchProvider;
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
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, LdapConstants.CONFIG, "Ldap Not Configured.", null);
        }
        Account account;
        Set<Identity> identities = ldapIdentitySearchProvider.getIdentities(username, password);
        Identity gotIdentity = null;
        for (Identity identity: identities){
            if (identity.getKind().equalsIgnoreCase(LdapConstants.USER_SCOPE)){
                gotIdentity = identity;
            }
        }
        if (gotIdentity == null) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(identities, false, null);
        if (SecurityConstants.SECURITY.get()) {
            ldapUtils.isAllowed(identities);
            account = authDao.getAccountByExternalId(gotIdentity.getExternalId(), LdapConstants.USER_SCOPE);
            if (null == account) {
                account = authDao.createAccount(username, AccountConstants.USER_KIND, gotIdentity.getExternalId(),
                        LdapConstants.USER_SCOPE);
                if (!hasAccessToAProject) {
                    projectResourceManager.createProjectForUser(account);
                }
            }
        } else {
            account = authDao.getAdminAccount();
            authDao.updateAccount(account, null, AccountConstants.ADMIN_KIND, gotIdentity.getExternalId(), LdapConstants.USER_SCOPE);
            authDao.ensureAllProjectsHaveNonRancherIdMembers(gotIdentity);
        }
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put(TokenUtils.TOKEN, LdapConstants.LDAP_JWT);
        jsonData.put(TokenUtils.ACCOUNT_ID, gotIdentity.getExternalId());
        jsonData.put(LdapConstants.USERNAME, username);
        jsonData.put(LdapConstants.LDAPUSERID, gotIdentity.getExternalId());
        List<String> groupsIdList = new ArrayList<>();
        for (Identity identity : identities) {
            groupsIdList.add(identity.getExternalId());
        }
        jsonData.put(LdapConstants.LDAP_GROUPS, groupsIdList);
        objectManager.persist(account);
        account = objectManager.reload(account);
        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS.get());
        String jwt = tokenService.generateEncryptedToken(jsonData, expiry);
        return new Token(jwt, SecurityConstants.AUTHPROVIDER.get(), accountId, gotIdentity, new ArrayList<>(identities), SecurityConstants.SECURITY.get());
    }

    @Override
    public Token createToken(ApiRequest request) throws IOException {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "LdapConfig", "LdapConfig is not Configured.", null);
        }
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(GithubConstants.GITHUB_REQUEST_CODE));
        String[] split = code.split(":");
        if (split.length != 2) {
            logger.error("Code is not 2 long. Meaning it is not username:pass : " + code);
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        return getLdapToken(split[0], split[1]);
    }

    @Override
    public boolean isConfigured() {
        return ldapIdentitySearchProvider.isConfigured();
    }

    @Override
    public String getName() {
        return LdapConstants.TOKEN_CREATOR;
    }
}
