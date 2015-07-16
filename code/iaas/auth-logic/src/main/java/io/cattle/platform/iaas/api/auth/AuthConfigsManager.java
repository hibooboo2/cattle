package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubConfig;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AuthConfig;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AuthConfigManager;
import io.cattle.platform.iaas.api.auth.integration.ldap.LdapConfig;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.Map;
import javax.inject.Inject;

public class AuthConfigsManager extends AbstractNoOpResourceManager {

    Map<String, AuthConfigManager> authConfigManagers;

    public Map<String, AuthConfigManager> getAuthConfigManagers() {
        return authConfigManagers;
    }

    @Inject
    public void setAuthConfigManagers(Map<String, AuthConfigManager> authConfigManagers) {
        this.authConfigManagers = authConfigManagers;
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{GithubConfig.class, LdapConfig.class};
    }

    @Override
    public Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        AuthConfig currentConfig;
        for (AuthConfigManager manager : authConfigManagers.values()) {
            currentConfig = manager.getCurrentConfig(null);
            if (currentConfig != null && currentConfig.isEnabled()) {
                return currentConfig;
            }
        }
        return false;
    }

}
