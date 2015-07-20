package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.integration.interfaces.AuthConfig;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = LdapConstants.CONFIG)
public class LdapConfig implements AuthConfig{

    private final boolean enabled;
    private final String server;
    private final int port;
    private final String loginDomain;
    private final String domain;
    private final String accessMode;
    private final String serviceAccountUsername;
    private final String serviceAccountPassword;
    private final boolean tls;
    private final String searchFieldUser;

    public LdapConfig(String server, int port, String loginDomain, String domain,
                      boolean enabled, String accessMode, String serviceAccountUsername,
                      String serviceAccountPassword, boolean tls, String searchFieldUser) {
        this.server = server;
        this.port = port;
        this.loginDomain = loginDomain;
        this.domain = domain;
        this.enabled = enabled;
        this.accessMode = accessMode;
        this.serviceAccountUsername = serviceAccountUsername;
        this.serviceAccountPassword = serviceAccountPassword;
        this.tls = tls;
        this.searchFieldUser = searchFieldUser;
    }

    @Field(required = true, nullable = false, minLength = 1)
    public String getServer() {
        return server;
    }

    @Field(nullable = false)
    public boolean getEnabled() {
        return enabled;
    }

    @Field(nullable = false, required = true, defaultValue = "389", minLength = 1)
    public int getPort() {
        return port;
    }

    @Field(required = true, nullable = false, minLength = 1)
    public String getLoginDomain() {
        return loginDomain;
    }

    @Field(required = true, nullable = false, minLength = 1)
    public String getDomain() {
        return domain;
    }

    @Field(required = true, nullable = true, defaultValue = "unrestricted")
    public String getAccessMode() {
        return accessMode;
    }

    @Override
    public boolean isConfigured() {
        return enabled;
    }

    @Override
    public String getName() {
        return LdapConstants.CONFIG;
    }

    @Field(nullable = false, required = true, minLength = 1)
    public String getServiceAccountUsername() {
        return serviceAccountUsername;
    }

    @Field(nullable = false, required = true, minLength = 1)
    public String getServiceAccountPassword() {
        return serviceAccountPassword;
    }

    @Field(nullable = false, required = true)
    public boolean getTls() {
        return tls;
    }

    @Field(nullable = true, required = false, defaultValue = "sAMAccountName")
    public String getSearchFieldUser() {
        return searchFieldUser;
    }
}
