package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = TokenUtils.TOKEN)
public class Token {

    private final String jwt;
    private final String accountId;
    private final String identityId;
    private String code;

    public Token(String jwt, String accountId, String identityId) {
        this.jwt = jwt;
        this.identityId = identityId;
        this.accountId = accountId;
    }

    @Field(nullable = true)
    public String getJwt() {
        return jwt;
    }

    @Field(required = true, nullable = true)
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
    public String getIdentityId() {
        return identityId;
    }
}
