package io.cattle.platform.iaas.api.auth.integration.github.resource;

import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.cattle.platform.iaas.api.auth.integration.github.GithubIdentitySearchProvider;
import io.cattle.platform.iaas.api.auth.integration.github.GithubUtils;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

public class GithubClient {

    @Inject
    private GithubUtils githubUtils;

    @Inject
    private JsonMapper jsonMapper;

    private static final Log logger = LogFactory.getLog(GithubClient.class);

    public GithubAccountInfo getUserAccountInfo(String githubAccessToken) throws IOException {
        if (StringUtils.isEmpty(githubAccessToken)) {
            return null;
        }
        Map<String, Object> jsonData;

        HttpResponse response = Request.Get(getURL(GithubClientEndpoints.USER_INFO))
                .addHeader(GithubConstants.AUTHORIZATION, "token " + githubAccessToken)
                .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readValue(response.getEntity().getContent());

        String accountId = ObjectUtils.toString(jsonData.get("id"));
        String accountName = ObjectUtils.toString(jsonData.get(GithubConstants.LOGIN));
        String profilePicture = ObjectUtils.toString(jsonData.get(GithubConstants.PROFILE_PICTURE));
        String profileUrl = ObjectUtils.toString(jsonData.get(GithubConstants.PROFILE_URL));
        return new GithubAccountInfo(accountId, accountName, profilePicture, profileUrl);
    }

    public List<GithubAccountInfo> getOrgAccountInfo(String githubAccessToken) throws IOException {
        if (StringUtils.isEmpty(githubAccessToken)) {
            return null;
        }
        List<GithubAccountInfo> orgInfoList = new ArrayList<>();
        List<Map<String, Object>> jsonData;

        HttpResponse response = Request.Get(getURL(GithubClientEndpoints.ORG_INFO))
                .addHeader(GithubConstants.AUTHORIZATION, "token " + githubAccessToken)
                .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);

        for (Map<String, Object> orgObject : jsonData) {
            String accountId = ObjectUtils.toString(orgObject.get("id"));
            String accountName = ObjectUtils.toString(orgObject.get(GithubConstants.LOGIN));
            String profilePicture = ObjectUtils.toString(orgObject.get(GithubConstants.PROFILE_PICTURE));
            String profileUrl = ObjectUtils.toString(orgObject.get(GithubConstants.PROFILE_URL));
            orgInfoList.add(new GithubAccountInfo(accountId, accountName, profilePicture, profileUrl));
        }
        return orgInfoList;
    }

    public List<TeamAccountInfo> getOrgTeamInfo(String githubAccessToken, String org) throws IOException {
        if (StringUtils.isEmpty(githubAccessToken)) {
            return null;
        }
        List<TeamAccountInfo> teamInfoList = new ArrayList<>();
        List<Map<String, Object>> jsonData;

        HttpResponse response = Request.Get(getURL(GithubClientEndpoints.ORGS) + org + "/teams").addHeader(GithubConstants.AUTHORIZATION, "token " +
                "" + githubAccessToken).addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON).execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            noGithub(statusCode);
        }
        jsonData = jsonMapper.readCollectionValue(response.getEntity().getContent(), List.class, Map.class);

        for (Map<String, Object> orgObject : jsonData) {
            String accountId = ObjectUtils.toString(orgObject.get("id"));
            String accountName = ObjectUtils.toString(orgObject.get(GithubConstants.NAMEFIELD));
            String slug = ObjectUtils.toString(orgObject.get("slug"));
            if (!StringUtils.equalsIgnoreCase("Owners", accountName)) {
                teamInfoList.add(new TeamAccountInfo(org, accountName, accountId, slug));
            }
        }
        return teamInfoList;
    }

    public GithubAccountInfo getUserIdByName(String username) {
        String githubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try {
            if (StringUtils.isEmpty(username)) {
                return null;
            }
            HttpResponse response = Request.Get(getURL(GithubClientEndpoints.USERS) + username)
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
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve UserId from Github", null);
        }

    }

    public GithubAccountInfo getOrgIdByName(String org) {
        String gitHubAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(GithubConstants.GITHUB_ACCESS_TOKEN);
        try {
            if (StringUtils.isEmpty(org)) {
                return null;
            }
            HttpResponse response = Request.Get(getURL(GithubClientEndpoints.ORGS) + org)
                    .addHeader(GithubConstants.ACCEPT, GithubConstants.APPLICATION_JSON)
                    .addHeader(GithubConstants.AUTHORIZATION, "token " + gitHubAccessToken).execute().returnResponse();
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
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "Could not retrieve orgId from Github", null);
        }
    }

    public String getTeamOrgById(String id) {
        return githubUtils.getTeamOrgById(id);
    }

    public void noGithub(Integer statusCode) {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, GithubConstants.GITHUB_ERROR,
                "Non-200 Response from Github", "Status code from Github: " + Integer.toString(statusCode));
    }

    public String getURL(GithubClientEndpoints val) {
        String hostName;
        String apiEndpoint;
        if (StringUtils.isBlank(GithubConstants.GITHUB_HOSTNAME.get())) {
            hostName = GithubConstants.GITHUB_DEFAULT_HOSTNAME;
            apiEndpoint = GithubConstants.GITHUB_API;
        } else {
            hostName = GithubConstants.SCHEME.get() + GithubConstants.GITHUB_HOSTNAME.get();
            apiEndpoint = GithubConstants.SCHEME.get() + GithubConstants.GITHUB_HOSTNAME.get() + GithubConstants.GHE_API;
        }
        String toReturn;
        switch (val) {
            case API:
                toReturn = apiEndpoint;
                break;
            case TOKEN:
                toReturn = hostName + "/login/oauth/access_token";
                break;
            case USERS:
                toReturn = apiEndpoint + "/users/";
                break;
            case ORGS:
                toReturn = apiEndpoint + "/orgs/";
                break;
            case USER_INFO:
                toReturn = apiEndpoint + "/user";
                break;
            case ORG_INFO:
                toReturn = apiEndpoint + "/user/orgs";
                break;
            case USER_PICTURE:
                toReturn = "https://avatars.githubusercontent.com/u/" + val + "?v=3&s=72";
                break;
            case USER_SEARCH:
                toReturn = apiEndpoint + "/search/users?q=";
                break;
            case TEAM:
                toReturn = apiEndpoint + "/teams/";
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "GithubIdentitySearchProvider", "Attempted to get invalid Api endpoint.", null);
        }
        return toReturn;
    }

    public boolean githubConfigured() {
        boolean githubConfigured = false;
        if (StringUtils.isNotBlank(GithubConstants.GITHUB_CLIENT_ID.get()) && StringUtils.isNotBlank(GithubConstants.GITHUB_CLIENT_SECRET.get())) {
            githubConfigured = true;
        }
        return githubConfigured;
    }

}
