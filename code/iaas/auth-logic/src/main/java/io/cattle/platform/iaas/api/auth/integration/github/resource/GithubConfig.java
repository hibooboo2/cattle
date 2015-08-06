package io.cattle.platform.iaas.api.auth.integration.github.resource;

import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = GithubConstants.GITHUBCONFIG)
public class GithubConfig {

    private String hostname;
    private String scheme;
    private Boolean enabled;
    private String accessMode;
    private String clientId;
    private String clientSecret;
    private List<String> allowedUsers;
    private List<String> allowedOrganizations;

    public GithubConfig(Boolean enabled, String accessMode, String clientId, List<String> allowedUsers, List<String> allowedOrganizations, String hostName,
                        String scheme) {
        this.enabled = enabled;
        this.accessMode = accessMode;
        this.clientId = clientId;
        this.allowedUsers = allowedUsers;
        this.allowedOrganizations = allowedOrganizations;
        this.hostname = hostName;
        this.scheme = scheme;
    }

    @Field(nullable = true)
    public Boolean getEnabled() {
        return enabled;
    }

    @Field(nullable = true)
    public String getClientId() {
        return clientId;
    }

    @Field(nullable = true)
    public String getClientSecret() {
        return clientSecret;
    }

    @Field(nullable = true)
    public List<String> getAllowedUsers() {
        return allowedUsers;
    }

    @Field(nullable = true)
    public List<String> getAllowedOrganizations() {
        return allowedOrganizations;
    }

    @Field(nullable = false)
    public String getAccessMode() {
        return accessMode;
    }

    @Field(nullable = true)
    public String getHostname() {
        return hostname;
    }

    @Field(nullable = true)
    public String getScheme() {
        return scheme;
    }
}
