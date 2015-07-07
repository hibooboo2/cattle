package io.cattle.platform.iaas.api.auth.providers.ldap;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = LdapConstants.LDAPCONFIG)
public class LdapConfig {

    private boolean enabled;
    private String server;
    private int port;
    private String loginDomain;
    private String domain;
    private String accessMode;

    public LdapConfig(String server, int port, String loginDomain, String domain, boolean enabled, String accessMode) {
        this.server = server;
        this.port = port;
        this.loginDomain = loginDomain;
        this.domain = domain;
        this.enabled = enabled;
        this.accessMode = accessMode;
    }

    @Field(nullable = false, minLength = 1)
    public String getServer() {
        return server;
    }

    @Field(nullable = true)
    public boolean getEnabled() {
        return enabled;
    }

    @Field(nullable = false, defaultValue = "389")
    public int getPort() {
        return port;
    }

    @Field(nullable = false, minLength = 1)
    public String getLoginDomain() {
        return loginDomain;
    }

    @Field(nullable = false, minLength = 1)
    public String getDomain() {
        return domain;
    }

    @Field(nullable = true, defaultValue = "unrestricted")
    public String getAccessMode() {
        return accessMode;
    }
}
