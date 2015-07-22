package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;


public class TokenResourceManager extends AbstractNoOpResourceManager {

    private List<TokenCreator> tokenCreators;

    public List<TokenCreator> getTokenCreators() {
        return tokenCreators;
    }

    @Inject
    public void setTokenCreators(List<TokenCreator> tokenCreators) {
        this.tokenCreators = tokenCreators;
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{Token.class};
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(TokenUtils.TOKEN, request.getType())) {
            return null;
        }
        try {
            return createToken(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Token createToken(ApiRequest request) throws IOException {
        Token token = null;
        List<ClientVisibleException> exceptions = new ArrayList<>();
        for (TokenCreator tokenCreator : tokenCreators) {
            try {
                token = tokenCreator.createToken(request);
            } catch (ClientVisibleException e) {
                exceptions.add(e);
            }
            if (token != null) {
                break;
            }
        }
        if (token == null && !exceptions.isEmpty()) {
            for (Exception e : exceptions) {
                e.printStackTrace();
            }
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "AuthorizationProvider", "Either No Authorzation Provider configured or " +
                    "invalid code.",
                    null);
        }
        return token;
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        //LEGACY: Used for old Implementation of projects/ Idnetities. Remove when vincent changes to new api.
        return new Token(SecurityConstants.SECURITY.get(), GithubConstants.GITHUB_CLIENT_ID.get(),
                GithubConstants.GITHUB_HOSTNAME.get(), SecurityConstants.AUTHPROVIDER.get());
    }
}
