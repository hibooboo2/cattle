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
    public List<Identity> searchIdentities(String name, String scope, boolean exactMatch) {
        //TODO:Implement exact match.
        if (!isConfigured()){
            return new ArrayList<>();
        }
        switch (scope){
            case GithubConstants.USER_SCOPE:
                return searchUsers(name, exactMatch);
            case GithubConstants.ORG_SCOPE:
                return searchGroups(name, exactMatch);
            case GithubConstants.TEAM_SCOPE:
                return searchTeams(name, exactMatch);
            default:
                return new ArrayList<>();
        }
    }

    private List<Identity> searchTeams(String teamName, boolean exactMatch) {
        return new ArrayList<>();
    }

    private List<Identity> searchGroups(String groupName, boolean exactMatch) {
        List<Identity> identities = new ArrayList<>();
        if (exactMatch) {
            GithubAccountInfo group;
            try {
                group =  githubClient.getGithubOrgByName(groupName);
                if (group == null){
                    return identities;
                }
            } catch (ClientVisibleException e) {
                return identities;
            }
            Identity identity = group.toIdentity(GithubConstants.ORG_SCOPE);
            identities.add(identity);
            return identities;
        }
        String url = githubClient.getURL(GithubClientEndpoints.USER_SEARCH) + groupName + "+type:org";
        List<Map<String, Object>> results = searchGithub(url);
        for (Map<String, Object> user: results){
            identities.add(new Identity(GithubConstants.ORG_SCOPE, String.valueOf(user.get("id")),
                    (String) user.get(GithubConstants.LOGIN), (String) user.get(GithubConstants.PROFILE_URL),
                    (String) user.get(GithubConstants.PROFILE_PICTURE)));
        }
        return identities;
    }

    @SuppressWarnings("unchecked")
    private List<Identity> searchUsers(String userName, boolean exactMatch) {
        List<Identity> identities = new ArrayList<>();
        if (exactMatch) {
            GithubAccountInfo user;
            try {
                user =  githubClient.getGithubUserByName(userName);
            } catch (ClientVisibleException e) {
                return identities;
            }
            Identity identity = user.toIdentity(GithubConstants.USER_SCOPE);
            identities.add(identity);
            return identities;
        }
        String url = githubClient.getURL(GithubClientEndpoints.USER_SEARCH) + userName + "+type:user";
        List<Map<String, Object>> results = searchGithub(url);
        for (Map<String, Object> user: results){
            identities.add(new Identity(GithubConstants.USER_SCOPE, String.valueOf(user.get("id")),
                    (String) user.get(GithubConstants.LOGIN), (String) user.get(GithubConstants.PROFILE_URL),
                    (String) user.get(GithubConstants.PROFILE_PICTURE)));
        }
        return identities;
    }

    private List<Map<String, Object>> searchGithub(String url) {
        try {
            HttpResponse res = Request.Get(url)
                    .addHeader("Authorization", "token " + githubUtils.getAccessToken()).addHeader
                            ("Accept", "application/json").execute().returnResponse();
            int statusCode = res.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                githubClient.noGithub(statusCode);
            }
            //TODO:Finish implementing search.
            Map<String, Object> jsonData = jsonMapper.readValue(res.getEntity().getContent());
            return (List<Map<String, Object>>) jsonData.get("items");
        } catch (IOException e) {
            //TODO: Propper Error Handling.
            return new ArrayList<>();
        }
    }

    @Override
    public Identity getIdentity(String id, String scope) {
        if (!isConfigured()){
            return null;
        }
        switch (scope) {
            case GithubConstants.USER_SCOPE:
                GithubAccountInfo user = getUserOrgById(id);
                return user == null ? null : user.toIdentity(GithubConstants.USER_SCOPE);
            case GithubConstants.ORG_SCOPE:
                GithubAccountInfo org = getUserOrgById(id);
                return org == null ? null : org.toIdentity(GithubConstants.ORG_SCOPE);
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
                githubClient.noGithub(statusCode);
            }
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));

            GithubAccountInfo org = githubClient.getGithubOrgByName(githubClient.getTeamOrgById(id));
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
                githubClient.noGithub(statusCode);
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
