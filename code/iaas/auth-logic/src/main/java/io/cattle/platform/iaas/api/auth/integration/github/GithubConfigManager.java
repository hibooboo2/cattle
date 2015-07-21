package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.SettingsUtils;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubConfig;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AuthConfig;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AuthConfigManager;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.netflix.config.DynamicStringProperty;

public class GithubConfigManager extends AbstractNoOpResourceManager implements AuthConfigManager {

    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String SCHEME = "scheme";
    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString(GithubConstants.CLIENT_ID_SETTING);

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ObjectManager objectManager;

    @Inject
    GithubClient client;

    @Inject
    SettingsUtils settingsUtils;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{GithubConfig.class};
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equalsIgnoreCase(GithubConstants.CONFIG, request.getType())) {
            return null;
        }
        Map<String, Object> config = jsonMapper.convertValue(request.getRequestObject(), Map.class);
        return updateCurrentConfig(config);
    }

    @SuppressWarnings("unchecked")
    public GithubConfig getCurrentConfig(Map<String, Object> config) {
        if (config == null){
            config = new HashMap<>();
        }
        boolean enabled = SecurityConstants.SECURITY.get();
        String clientId = GITHUB_CLIENT_ID.get();
        String accessMode = GithubConstants.ACCESS_MODE.get();
        String hostname = GithubConstants.GITHUB_HOSTNAME.get();
        String scheme = GithubConstants.SCHEME.get();
        List<String> allowedUsers = getAccountNames(fromCommaSeparatedString(GithubConstants.GITHUB_ALLOWED_USERS.get()));
        List<String> allowedOrgs = getAccountNames(fromCommaSeparatedString(GithubConstants.GITHUB_ALLOWED_ORGS.get()));
        if (config.get(SecurityConstants.ENABLED) != null) {
            enabled = (Boolean) config.get(SecurityConstants.ENABLED);
        }
        if (config.get(TokenUtils.ACCESSMODE) != null) {
            accessMode = (String) config.get(TokenUtils.ACCESSMODE);
        }
        if (config.get(GithubConstants.HOSTNAME) != null) {
            hostname = (String) config.get(GithubConstants.HOSTNAME);
        }
        if (config.get(SCHEME) != null) {
            scheme = (String) config.get(SCHEME);
        }
        if (config.get(CLIENT_ID) != null) {
            clientId = (String) config.get(CLIENT_ID);
        }
        if (config.get(GithubConstants.ALLOWED_USERS) != null) {
            allowedUsers = (List<String>) config.get(GithubConstants.ALLOWED_USERS);
        }
        if (config.get(GithubConstants.ALLOWED_ORGS) != null) {
            allowedOrgs = (List<String>) config.get(GithubConstants.ALLOWED_ORGS);
        }
        return new GithubConfig(enabled, accessMode, clientId, allowedUsers, allowedOrgs, hostname, scheme);
    }

    protected List<String> appendUserIds(List<String> usernames) {
        if (usernames == null) {
            return null;
        }
        List<String> appendedList = new ArrayList<>();

        for (String username : usernames) {
            GithubAccountInfo userInfo = client.getUserIdByName(username);
            if (userInfo == null) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidUsername", "Invalid username: " + username, null);
            }
            appendedList.add(userInfo.toString());
        }
        return appendedList;
    }

    protected List<String> appendOrgIds(List<String> orgs) {
        if (orgs == null) {
            return null;
        }
        List<String> appendedList = new ArrayList<>();
        for (String org : orgs) {
            GithubAccountInfo orgInfo = client.getOrgIdByName(org);
            if (orgInfo == null) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidOrganization", "Invalid organization: " + org, null);
            }
            appendedList.add(orgInfo.toString());
        }
        return appendedList;
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return getCurrentConfig(new HashMap<String, Object>());
    }

    private List<String> getAccountNames(List<String> accountInfos) {
        if (accountInfos == null) {
            return null;
        }
        return Lists.transform(accountInfos, new Function<String, String>() {

            @Override
            public String apply(String accountInfo) {
                String[] accountInfoArr = accountInfo.split("[:]");
                return accountInfoArr[0];
            }

        });
    }

    protected List<String> fromCommaSeparatedString(String string) {
        if (StringUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        List<String> strings = new ArrayList<String>();
        String[] splitted = string.split(",");
        for (int i = 0; i < splitted.length; i++) {
            String element = splitted[i].trim();
            strings.add(element);
        }
        return strings;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AuthConfig updateCurrentConfig(Map<String, Object> config) {
        settingsUtils.changeSetting(GithubConstants.HOSTNAME_SETTING, config.get(GithubConstants.HOSTNAME));
        settingsUtils.changeSetting(GithubConstants.SCHEME_SETTING, config.get(SCHEME));
        settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, config.get(SecurityConstants.ENABLED));
        settingsUtils.changeSetting(GithubConstants.CLIENT_ID_SETTING, config.get(CLIENT_ID));
        if (config.get(CLIENT_SECRET) != null) {
            settingsUtils.changeSetting(GithubConstants.CLIENT_SECRET_SETTING, config.get(CLIENT_SECRET));
        }
        settingsUtils.changeSetting(GithubConstants.ACCESSMODE_SETTING, config.get(TokenUtils.ACCESSMODE));
        settingsUtils.changeSetting(GithubConstants.ALLOWED_USERS_SETTING,
                StringUtils.join(appendUserIds((List<String>) config.get(GithubConstants.ALLOWED_USERS)), ","));
        settingsUtils.changeSetting(GithubConstants.ALLOWED_ORGS_SETTING,
                StringUtils.join(appendOrgIds((List<String>) config.get(GithubConstants.ALLOWED_ORGS)), ","));
        if (config.get(SecurityConstants.ENABLED) != null && (boolean) config.get(SecurityConstants.ENABLED)){
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, GithubConstants.CONFIG);
        } else {
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
        }
        return getCurrentConfig(config);
    }

    @Override
    public String getName() {
        return GithubConstants.MANAGER;
    }

    @Override
    public boolean isConfigured() {
        return client.githubConfigured();
    }
}
