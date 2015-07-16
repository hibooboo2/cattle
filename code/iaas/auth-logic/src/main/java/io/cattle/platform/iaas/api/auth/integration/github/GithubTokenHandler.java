package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.github.resource.TeamAccountInfo;
import io.cattle.platform.iaas.api.auth.integration.github.resource.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenHandler;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

import com.netflix.config.DynamicLongProperty;

public class GithubTokenHandler implements TokenHandler {

    private static final DynamicLongProperty TOKEN_EXPIRY_MILLIS = ArchaiusUtil.getLong("api.auth.jwt.token.expiry");
    @Inject
    ProjectResourceManager projectResourceManager;
    @Inject
    ObjectManager objectManager;
    @Inject
    GithubUtils githubUtils;
    @Inject
    GithubClient githubClient;
    @Inject
    private TokenService tokenService;
    @Inject
    private AuthDao authDao;
    @Inject
    private GithubIdentitySearchProvider githubIdentitySearchProvider;

    public Token getGithubToken(String accessToken) throws IOException {
        List<String> idList = new ArrayList<>();
        List<String> orgNames = new ArrayList<>();
        List<String> teamIds = new ArrayList<>();
        List<String> orgIds = new ArrayList<>();
        Map<String, String> teamToOrg = new HashMap<>();
        List<TeamAccountInfo> teamsAccountInfo = new ArrayList<>();
        GithubAccountInfo userAccountInfo = githubClient.getUserAccountInfo(accessToken);
        List<GithubAccountInfo> orgAccountInfo = githubClient.getOrgAccountInfo(accessToken);
        Set<Identity> identities = new HashSet<>();

        idList.add(userAccountInfo.getAccountId());
        identities.add(new Identity(GithubConstants.USER_SCOPE, userAccountInfo.getAccountId(),
                userAccountInfo.getAccountName(), userAccountInfo.getProfileUrl(), userAccountInfo.getProfilePicture()));

        for (GithubAccountInfo info : orgAccountInfo) {
            idList.add(info.getAccountId());
            orgNames.add(info.getAccountName());
            orgIds.add(info.getAccountId());
            teamsAccountInfo.addAll(githubClient.getOrgTeamInfo(accessToken, info.getAccountName()));
            identities.add(new Identity(GithubConstants.ORG_SCOPE, info.getAccountId(),
                    info.getAccountName(), info.getProfileUrl(), info.getProfilePicture()));
        }

        for (TeamAccountInfo info : teamsAccountInfo) {
            teamIds.add(info.getId());
            teamToOrg.put(info.getId(), info.getOrg());
            idList.add(info.getId());
            identities.add(new Identity(GithubConstants.TEAM_SCOPE, info.getId(), info.getOrg() + ":" + info.getName()));
        }

        Account account = null;
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(identities, false, null);
        if (SecurityConstants.SECURITY.get()) {
            if (githubUtils.isAllowed(idList, identities)) {
                account = authDao.getAccountByExternalId(userAccountInfo.getAccountId(), GithubConstants.USER_SCOPE);
                if (null == account) {
                    account = authDao.createAccount(userAccountInfo.getAccountName(), AccountConstants.USER_KIND, userAccountInfo.getAccountId(),
                            GithubConstants.USER_SCOPE);
                    if (!hasAccessToAProject) {
                        projectResourceManager.createProjectForUser(account);
                    }
                }
            }
        } else {
            account = authDao.getAdminAccount();
            authDao.updateAccount(account, null, AccountConstants.ADMIN_KIND, userAccountInfo.getAccountId(), GithubConstants.USER_SCOPE);
            authDao.ensureAllProjectsHaveNonRancherIdMembers(new Identity(GithubConstants.USER_SCOPE, userAccountInfo.getAccountId(),
                    userAccountInfo.getAccountName()));
        }
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put(TokenUtils.TOKEN, GithubConstants.GITHUB_JWT);
        jsonData.put(TokenUtils.ACCOUNT_ID, userAccountInfo.getAccountId());
        jsonData.put("teamToOrg", teamToOrg);
        jsonData.put(GithubConstants.USERNAME, userAccountInfo.getAccountName());
        jsonData.put(GithubConstants.TEAM_IDS, teamIds);
        jsonData.put(GithubConstants.ORG_IDS, orgIds);
        jsonData.put(GithubConstants.ID_LIST, idList);
        DataAccessor.fields(account).withKey(GithubConstants.GITHUB_ACCESS_TOKEN).set(accessToken);
        objectManager.persist(account);
        account = objectManager.reload(account);
        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS.get());
        String jwt = tokenService.generateEncryptedToken(jsonData, expiry);
        return new Token(jwt, userAccountInfo.getAccountName(), orgNames, teamsAccountInfo, null, null,
                account.getKind(), accountId);
    }

    @Override
    public Token getToken(ApiRequest request) throws IOException {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        String code = ObjectUtils.toString(requestBody.get(GithubConstants.GITHUB_REQUEST_CODE));
        String accessToken = githubIdentitySearchProvider.getAccessToken(code);
        return getGithubToken(accessToken);
    }


}
