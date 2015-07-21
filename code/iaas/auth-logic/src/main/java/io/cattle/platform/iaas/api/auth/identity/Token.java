package io.cattle.platform.iaas.api.auth.identity;

        import io.cattle.platform.api.auth.Identity;
        import io.cattle.platform.iaas.api.auth.TokenUtils;
        import io.cattle.platform.iaas.api.auth.integration.github.resource.TeamAccountInfo;
        import io.github.ibuildthecloud.gdapi.annotation.Field;
        import io.github.ibuildthecloud.gdapi.annotation.Type;

        import java.util.List;


@Type(name = TokenUtils.TOKEN)
public class Token {

    private final String jwt;
    private final String hostname;
    private String code;
    private final String user;
    private final List<String> orgs;
    private final List<TeamAccountInfo> teams;
    private final Boolean security;
    private final String clientId;
    private final String userType;
    private final String authProvider;

    private final String accountId;
    private final Identity userIdentity;
    private final boolean enabled;
    private final List<Identity> identities;

    public Token(String jwt, String username, List<String> orgs, List<TeamAccountInfo> teams, Boolean security, String clientId, String userType,
                 String authProvider, String accountId, List<Identity> identities) {
        this.jwt = jwt;
        this.user = username;
        this.authProvider = authProvider;
        this.hostname = null;
        this.orgs = orgs;
        this.teams = teams;
        this.security = security;
        this.clientId = clientId;
        this.userType = userType;
        this.accountId = accountId;
        this.userIdentity = null;
        this.enabled = this.security;
        this.identities = identities;

    }

    public Token(Boolean security, String clientId, String hostName, String authProvider) {
        this.authProvider = authProvider;
        this.jwt = null;
        this.user = null;
        this.orgs = null;
        this.teams = null;
        this.security = security;
        this.clientId = clientId;
        this.userType = null;
        this.accountId = null;
        this.hostname = hostName;
        this.userIdentity = null;
        enabled = this.security;
        identities = null;
    }

    public Token(String jwt, String authProvider, String accountId, Identity userIdentity, List<Identity> identities, boolean enabled) {
        this.jwt = jwt;
        this.authProvider = authProvider;
        this.userIdentity = userIdentity;
        this.accountId = accountId;
        this.identities = identities;
        this.enabled = enabled;
        this.user = userIdentity.getName();
        this.hostname = null;
        this.orgs = null;
        this.teams = null;
        this.security = null;
        this.clientId = null;
        this.userType = userIdentity.getKind();
    }

    public Token(String authProvider, boolean enabled) {
        this.authProvider = authProvider;
        this.enabled = enabled;
        this.jwt = null;
        this.accountId = null;
        this.user = null;
        this.identities = null;
        this.hostname = null;
        this.orgs = null;
        this.teams = null;
        this.security = null;
        this.clientId = null;
        this.userType = null;
        this.userIdentity = null;
    }

    @Field(nullable = true)
    public String getJwt() {
        return jwt;
    }

    @Field(nullable = true)
    public void setCode(String code) {
        this.code = code;
    }

    @Field(nullable = true)
    public String getUser() {
        return user;
    }

    @Field(nullable = true)
    public List<String> getOrgs() {
        return orgs;
    }

    @Field(nullable = true)
    public List<TeamAccountInfo> getTeams() {
        return teams;
    }

    @Field(nullable = true)
    public Boolean getSecurity() {
        return security;
    }

    @Field(nullable = true)
    public String getClientId() {
        return clientId;
    }

    @Field(nullable = true)
    public String getUserType() {
        return userType;
    }

    @Field(nullable = true)
    public String getAccountId() {
        return accountId;
    }

    @Field(nullable = true)
    public String getHostname() {
        return hostname;
    }


    @Field(nullable = true, required = true)
    public String getCode() {
        return code;
    }

    @Field(nullable = true)
    public Identity getUserIdentity() {
        return userIdentity;
    }

    @Field(nullable = true)
    public boolean isEnabled() {
        return enabled;
    }

    @Field(nullable = true)
    public Identity[] getIdentities() {
        return (Identity[]) identities.toArray();
    }

    @Field(nullable = true)
    public String getAuthProvider() {
        return authProvider;
    }
}
