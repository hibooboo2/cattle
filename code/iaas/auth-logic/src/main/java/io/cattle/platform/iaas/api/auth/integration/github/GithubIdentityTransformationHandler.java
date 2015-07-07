package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityTransformationHandler;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubAccountInfo;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class GithubIdentityTransformationHandler extends GithubConfigurable implements IdentityTransformationHandler {


    @Inject
    GithubIdentitySearchProvider githubIdentitySearchProvider;
    @Inject
    GithubUtils githubUtils;
    @Inject
    GithubTokenCreator githubTokenCreator;
    @Inject
    GithubClient githubClient;

    @Override
    public Identity transform(Identity identity) {
        GithubAccountInfo githubAccountInfo;
        switch (identity.getExternalIdType()) {
            case GithubConstants.USER_SCOPE:
                githubAccountInfo = githubClient.getGithubUserByName(identity.getExternalId());
                return githubAccountInfo.toIdentity(GithubConstants.USER_SCOPE);
            case GithubConstants.ORG_SCOPE:
                githubAccountInfo = githubClient.getGithubOrgByName(identity.getExternalId());
                return githubAccountInfo.toIdentity(GithubConstants.ORG_SCOPE);
            case GithubConstants.TEAM_SCOPE:
                String org = githubClient.getTeamOrgById(identity.getExternalId());
                return new Identity(GithubConstants.TEAM_SCOPE, identity.getExternalId(), org + ":" + identity.getExternalId(),
                        null, null, null);
            default:
                return null;
        }
    }

    @Override
    public Identity untransform(Identity identity) {
        switch (identity.getExternalIdType()) {
            case GithubConstants.USER_SCOPE:
            case GithubConstants.ORG_SCOPE:
            case GithubConstants.TEAM_SCOPE:
                return identity;
            default:
                return null;
        }
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        if (!isConfigured()) {
            return new HashSet<>();
        }
        ApiRequest request = ApiContext.getContext().getApiRequest();
        githubUtils.findAndSetJWT();
        String jwt = githubUtils.getJWT();
        String accessToken = (String) DataAccessor.fields(account).withKey(GithubConstants.GITHUB_ACCESS_TOKEN).get();
        if (StringUtils.isBlank(jwt) && !StringUtils.isBlank(accessToken)) {
            try {
                jwt = ProjectConstants.AUTH_TYPE + githubTokenCreator.getGithubToken(accessToken).getJwt();
            } catch (ClientVisibleException e) {
                if (e.getCode().equalsIgnoreCase(GithubConstants.GITHUB_ERROR) &&
                        !e.getDetail().contains("401")) {
                    throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                            GithubConstants.JWT_CREATION_FAILED, "", null);
                }
            } catch (IOException e) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        GithubConstants.JWT_CREATION_FAILED, "", null);
            }
        }
        if (jwt != null && !jwt.isEmpty()) {
            request.setAttribute(GithubConstants.GITHUB_JWT, jwt);
            request.setAttribute(GithubConstants.GITHUB_ACCESS_TOKEN, accessToken);
            return githubUtils.getIdentities();
        }
        return new HashSet<>();
    }

    @Override
    public String getName() {
        return GithubConstants.TRANSFORMATION_HANDLER;
    }
}
