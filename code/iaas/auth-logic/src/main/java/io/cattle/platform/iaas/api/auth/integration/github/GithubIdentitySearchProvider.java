package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.identity.AbstractIdentitySearchProvider;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClientEndpoints;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;

public class GithubIdentitySearchProvider extends AbstractIdentitySearchProvider {

    private static final String LOGIN = "login";

    @Inject
    GithubClient githubClient;

    @Inject
    private JsonMapper jsonMapper;
    @Inject
    private GithubUtils githubUtils;

    public String getAccessToken(String code) throws IOException {
        List<BasicNameValuePair> requestData = new ArrayList<>();

        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, GithubConstants.CONFIG, "No Github Client id and secret found.", null);
        }

        requestData.add(new BasicNameValuePair(GithubConstants.CLIENT_ID, GithubConstants.GITHUB_CLIENT_ID.get()));
        requestData.add(new BasicNameValuePair(GithubConstants.CLIENT_SECRET, GithubConstants.GITHUB_CLIENT_SECRET.get()));
        requestData.add(new BasicNameValuePair(GithubConstants.GITHUB_REQUEST_CODE, code));

        Map<String, Object> jsonData;

        HttpResponse response = Request.Post(githubClient.getURL(GithubClientEndpoints.TOKEN))
                .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).bodyForm(requestData)
                .execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            githubClient.noGithub(statusCode);
        }
        jsonData = jsonMapper.readValue(response.getEntity().getContent());

        if (jsonData.get("error") != null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, (String) jsonData.get("error_description"));
        }

        return (String) jsonData.get(GithubConstants.ACCESS_TOKEN);
    }

    @Override
    public List<Identity> searchIdentities(String name, String scope) {
        if (!isConfigured()){
            return new ArrayList<>();
        }
        HttpResponse res;
        try {
            res = Request.Get(githubClient.getURL(GithubClientEndpoints.USER_SEARCH) + name)
                    .addHeader("Authorization", "token " + githubUtils.getAccessToken()).addHeader
                    ("Accept", "application/json").execute().returnResponse();
            int statusCode = res.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                githubClient.noGithub(statusCode);
            }
            //TODO:Finish implementing search.
            Map<String, Object> jsonData = jsonMapper.readValue(res.getEntity().getContent());
            jsonData.toString();
        } catch (IOException e) {
            //TODO: Propper Error Handling.
            return null;
        }
        List<Identity> identities = new ArrayList<>();
        identities.add(new Identity(scope, res.toString()));
        return identities;
    }

    @Override
    public Identity getIdentity(String id, String scope) {
        if (!isConfigured()){
            return null;
        }
        switch (scope) {
            case GithubConstants.USER_SCOPE:
                GithubAccountInfo user = getUserOrgById(id);
                return new Identity(GithubConstants.USER_SCOPE, user.getAccountId(),
                        user.getAccountName(), user.getProfileUrl(), user.getProfilePicture());
            case GithubConstants.ORG_SCOPE:
                GithubAccountInfo org = getUserOrgById(id);
                return new Identity(GithubConstants.ORG_SCOPE, org.getAccountId(),
                        org.getAccountName(), org.getProfileUrl(), org.getProfilePicture());
            case GithubConstants.TEAM_SCOPE:
                return getTeamById(id);
            default:
                return null;
        }
    }

    private Identity getTeamById(String id) {
        if (!isConfigured()) {
            return null;
        }
        String gitHubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try {
            if (StringUtils.isEmpty(id)) {
                return null;
            }
            HttpResponse response = Request.Get(githubClient.getURL(GithubClientEndpoints.TEAM) + id)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON)
                    .addHeader(GithubConstants.AUTHORIZATION, "token " + gitHubAccessToken).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, GithubConstants.GITHUB_ERROR,
                        "Non-200 Response from Github", "Status code from Github: " + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));

            GithubAccountInfo org = githubClient.getOrgIdByName(githubClient.getTeamOrgById(id));
            String accountId = ObjectUtils.toString(jsonData.get("id"));
            String accountName = ObjectUtils.toString(jsonData.get(GithubConstants.NAMEFIELD));
            String profilePicture = org.getProfilePicture();
            String profileUrl = org.getProfileUrl();
            return new Identity(GithubConstants.TEAM_SCOPE, accountId, org.getAccountName() + ':' + accountName,
                    profileUrl, profilePicture);
        } catch (IOException e) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve orgId from Github", null);
        }
    }

    private GithubAccountInfo getUserOrgById(String id) {
        String githubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try {
            if (StringUtils.isEmpty(id)) {
                return null;
            }
            HttpResponse response = Request.Get(githubClient.getURL(GithubClientEndpoints.USER_INFO) + '/' + id)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).addHeader(
                            GithubConstants.AUTHORIZATION, "token " + githubAccessToken).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, GithubConstants.GITHUB_ERROR,
                        "Non-200 Response from Github", "Status code from Github: " + Integer.toString(statusCode));
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));

            String accountId = ObjectUtils.toString(jsonData.get("id"));
            String accountName = ObjectUtils.toString(jsonData.get(GithubConstants.LOGIN));
            String profilePicture = ObjectUtils.toString(jsonData.get(GithubConstants.PROFILE_PICTURE));
            String profileUrl = ObjectUtils.toString(jsonData.get(GithubConstants.PROFILE_URL));
            return new GithubAccountInfo(accountId, accountName, profilePicture, profileUrl);
        } catch (IOException e) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve UserId from Github", null);
        }
    }

    @Override
    public List<String> scopesProvided() {
        if (!isConfigured()){
            return new ArrayList<>();
        }
        return Arrays.asList(GithubConstants.SCOPES);
    }

    @Override
    public boolean isConfigured() {
        return githubClient.githubConfigured();
    }

    @Override
    public String getName() {
        return GithubConstants.SEARCH_PROVIDER;
    }
}
