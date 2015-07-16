package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = TokenUtils.TOKEN)
public class Token {

    private final String jwt;
    private final String accountId;
    private final Identity user;
    private final boolean enabled;
    private final List<Identity> identities;
    private String code;

    public Token(String jwt, String accountId, Identity user, List<Identity> identities, boolean enabled) {
        this.jwt = jwt;
        this.user = user;
        this.accountId = accountId;
        this.identities = identities;
        identities.remove(user);
        this.enabled = enabled;
    }

    public Token(boolean enabled) {
        this.enabled = enabled;
        this.jwt = null;
        this.accountId = null;
        this.user = null;
        this.identities = null;
    }

    @Field(nullable = true)
    public String getJwt() {
        return jwt;
    }

    @Field(nullable = true)
    public String getCode() {
        return code;
    }

    @Field(nullable = true)
    public void setCode(String code) {
        this.code = code;
    }

    @Field(nullable = true)
    public String getAccountId() {
        return accountId;
    }

    @Field(nullable = true)
    public Identity getUser() {
        return user;
    }

    @Field(nullable = true)
    public boolean isEnabled() {
        return enabled;
    }

    @Field(nullable = true)
    public List<Identity> getIdentities() {
        return identities;
    }
}
