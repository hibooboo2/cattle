package io.cattle.platform.iaas.api.auth.integrations.github;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.AuthUtils;
import io.cattle.platform.iaas.api.auth.integrations.github.constants.GithubConstants;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.netflix.config.DynamicStringProperty;

public class GithubUtils extends AuthUtils {

    private static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString("api.auth.github.access.mode");
    private static final DynamicStringProperty WHITELISTED_ORGS = ArchaiusUtil.getString("api.auth.github.allowed.orgs");
    private static final DynamicStringProperty WHITELISTED_USERS = ArchaiusUtil.getString("api.auth.github.allowed.users");


    @Inject
    GithubClient githubClient;

    @SuppressWarnings("unchecked")
    @Override
    protected Set<ExternalId> externalIds(Map<String, Object> jsonData) {
        Set<ExternalId> externalIds = new HashSet<>();
        if (jsonData == null) {
            return externalIds;
        }
        List<String> teamIds = (List<String>) CollectionUtils.toList(jsonData.get(GithubConstants.TEAM_IDS));
        List<String> orgIds = (List<String>) CollectionUtils.toList(jsonData.get(GithubConstants.ORG_IDS));
        String accountId = ObjectUtils.toString(jsonData.get(AuthUtils.ACCOUNT_ID), null);
        externalIds.add(new ExternalId(accountId, (String) jsonData.get(GithubConstants.USERNAME)));
        for (String teamId : teamIds) {
            externalIds.add(new ExternalId(teamId));
        }
        for (String orgId : orgIds) {
            externalIds.add(new ExternalId(orgId));
        }
        return externalIds;
    }

    @Override
    protected String accessMode() {
        return ACCESS_MODE.get();
    }

    @Override
    protected String accessToken() {
        return GithubConstants.GITHUB_ACCESS_TOKEN;
    }

    @SuppressWarnings("unchecked")
    public String getTeamOrgById(String id) {
        Map<String, Object> jsonData = getJsonData();
        Map<String, String> teamToOrg = (Map<String, String>) jsonData.get("teamToOrg");
        return teamToOrg.get(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean isAllowed(Map<String, Object> jsonData) {
        List<String> idList = (List<String>) jsonData.get(GithubConstants.ID_LIST);
        Set<ExternalId> externalIds = externalIds(jsonData);
        return isAllowed(idList, externalIds);
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return false;
        }
        List<String> whitelistedValues = fromCommaSeparatedString(WHITELISTED_ORGS.get());
        whitelistedValues.addAll(fromCommaSeparatedString(WHITELISTED_USERS.get()));
        Collection<String> whitelistedIds = Collections2.transform(whitelistedValues, new Function<String, String>() {
            @Override
            public String apply(String arg) {
                return arg.split("[:]")[1];
            }
        });
        Set<String> whitelist = new HashSet<>(whitelistedIds);
        for (String id : idList) {
            if (whitelist.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private List<String> fromCommaSeparatedString(String string) {
        if (StringUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        List<String> strings = new ArrayList<>();
        String[] splitted = string.split(",");
        for (String aSplitted : splitted) {
            String element = aSplitted.trim();
            strings.add(element);
        }
        return strings;
    }

    @Override
    protected String getAccountType() {
        return GithubConstants.USER_SCOPE;
    }

    @Override
    protected String tokenType() {
        return GithubConstants.GITHUB_JWT;
    }
}
