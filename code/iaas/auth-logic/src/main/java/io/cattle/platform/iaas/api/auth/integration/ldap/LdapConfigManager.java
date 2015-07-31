package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.SettingsUtils;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AuthConfig;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AuthConfigManager;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class LdapConfigManager extends AbstractNoOpResourceManager implements AuthConfigManager{


    @Inject
    SettingsUtils settingsUtils;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    private LdapIdentitySearchProvider ldapClient;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{LdapConfig.class};
    }


    @SuppressWarnings("unchecked")
    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(LdapConstants.CONFIG, request.getType())) {
            return null;
        }
        Map<String, Object> config = jsonMapper.convertValue(request.getRequestObject(), Map.class);
        return updateCurrentConfig(config);
    }

    @SuppressWarnings("unchecked")
    private LdapConfig currentLdapConfig(Map<String, Object> config) {
        LdapConfig currentConfig = (LdapConfig) listInternal(null, null, null, null);
        String domain = currentConfig.getDomain();
        if (config.get(LdapConstants.DOMAIN) != null) {
            domain = (String) config.get(domain);
        }
        String server = currentConfig.getServer();
        if (config.get(LdapConstants.SERVER) != null) {
            server = (String) config.get(LdapConstants.SERVER);
        }
        String loginDomain = currentConfig.getLoginDomain();
        if (config.get(LdapConstants.LOGIN_DOMAIN) != null) {
            loginDomain = (String) config.get(LdapConstants.LOGIN_DOMAIN);
        }
        String accessMode = currentConfig.getAccessMode();
        if (config.get(LdapConstants.ACCESSMODE) != null) {
            loginDomain = (String) config.get(LdapConstants.ACCESSMODE);
        }
        String serviceAccountUsername = currentConfig.getServiceAccountUsername();
        if (config.get(LdapConstants.SERVICE_ACCOUNT_USERNAME) != null) {
            loginDomain = (String) config.get(LdapConstants.SERVICE_ACCOUNT_USERNAME);
        }
        String serviceAccountPassword = currentConfig.getServiceAccountPassword();
        if (config.get(LdapConstants.SERVICE_ACCOUNT_PASSWORD) != null) {
            loginDomain = (String) config.get(LdapConstants.SERVICE_ACCOUNT_PASSWORD);
        }
        boolean tls = currentConfig.getTls();
        if (config.get(LdapConstants.TLS) != null) {
            tls = (Boolean) config.get(LdapConstants.TLS);
        }
        int port = currentConfig.getPort();
        if (config.get(LdapConstants.PORT) != null) {
            port = (int) (long) config.get(LdapConstants.PORT);
        }
        boolean enabled = currentConfig.getEnabled();
        if (config.get(SecurityConstants.ENABLED) != null) {
            enabled = (Boolean) config.get(SecurityConstants.ENABLED);
        }
        String userSearchField = currentConfig.getUserSearchField();
        if (config.get(LdapConstants.USER_SEARCH_FIELD) != null){
            userSearchField = (String) config.get(LdapConstants.USER_SEARCH_FIELD);
        }
        String groupSearchField = currentConfig.getGroupSearchField();
        if (config.get(LdapConstants.GROUP_SEARCH_FIELD) != null){
            groupSearchField = (String) config.get(LdapConstants.GROUP_SEARCH_FIELD);
        }
        String userLoginField = currentConfig.getUserLoginField();
        if (config.get(LdapConstants.USER_LOGIN_FIELD) != null){
            groupSearchField = (String) config.get(LdapConstants.USER_LOGIN_FIELD);
        }
        return new LdapConfig(server, port, loginDomain, domain, enabled, accessMode, serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, groupSearchField);
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        boolean enabled = SecurityConstants.SECURITY.get();
        boolean tls = LdapConstants.TLS_ENABLED.get();

        String server = LdapConstants.LDAP_SERVER.get();
        String loginDomain = LdapConstants.LDAP_LOGIN_DOMAIN.get();
        String domain = LdapConstants.LDAP_DOMAIN.get();
        String accessMode = LdapConstants.ACCESS_MODE.get();
        String serviceAccountPassword = LdapConstants.SERVICEACCOUNT_PASSWORD.get();
        String serviceAccountUsername = LdapConstants.SERVICEACCOUNT_USER.get();
        String userSearchField = LdapConstants.USER_SEARCHFIELD.get();
        String groupSearchField = LdapConstants.GROUP_SEARCHFIELD.get();
        String userLoginField = LdapConstants.USER_LOGINFIELD.get();
        int port;
        if (LdapConstants.LDAP_PORT.get() == null) {
            port = 389;
        } else {
            port = Integer.valueOf(LdapConstants.LDAP_PORT.get());
        }
        return new LdapConfig(server, port, loginDomain, domain, enabled, accessMode, serviceAccountUsername, serviceAccountPassword, tls, userSearchField, userLoginField, groupSearchField);
    }

    @Override
    public AuthConfig getCurrentConfig(Map<String, Object> config) {
        return currentLdapConfig(config);
    }

    @Override
    public AuthConfig updateCurrentConfig(Map<String, Object> config) {
        settingsUtils.changeSetting(LdapConstants.DOMAIN_SETTING, config.get(LdapConstants.DOMAIN));
        settingsUtils.changeSetting(LdapConstants.ACCESS_MODE_SETTING, config.get(LdapConstants.ACCESSMODE));
        settingsUtils.changeSetting(LdapConstants.SERVER_SETTING, config.get(LdapConstants.SERVER));
        settingsUtils.changeSetting(LdapConstants.LOGIN_DOMAIN_SETTING, config.get(LdapConstants.LOGIN_DOMAIN));
        settingsUtils.changeSetting(LdapConstants.USER_SEARCH_FIELD_SETTING, config.get(LdapConstants.USER_SEARCH_FIELD));
        settingsUtils.changeSetting(LdapConstants.GROUP_SEARCH_FIELD_SETTING, config.get(LdapConstants.GROUP_SEARCH_FIELD));
        settingsUtils.changeSetting(LdapConstants.USER_LOGIN_FIELD_SETTING, config.get(LdapConstants.USER_LOGIN_FIELD));
        settingsUtils.changeSetting(LdapConstants.PORT_SETTING, config.get(LdapConstants.PORT));
        settingsUtils.changeSetting(LdapConstants.SERIVCE_ACCOUNT_USERNAME_SETTING, config.get(LdapConstants.SERVICE_ACCOUNT_USERNAME));
        settingsUtils.changeSetting(LdapConstants.SERVICE_ACCOUNT_PASSWORD_SETTING, config.get(LdapConstants.SERVICE_ACCOUNT_PASSWORD));
        settingsUtils.changeSetting(LdapConstants.TLS_SETTING, config.get(LdapConstants.TLS));
        settingsUtils.changeSetting(SecurityConstants.SECURITY_SETTING, config.get(SecurityConstants.ENABLED));
        if (config.get(SecurityConstants.ENABLED) != null && (boolean) config.get(SecurityConstants.ENABLED)){
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, LdapConstants.CONFIG);
        } else {
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
        }
        return currentLdapConfig(config);
    }

    @Override
    public String getName() {
        return LdapConstants.MANAGER;
    }

}
