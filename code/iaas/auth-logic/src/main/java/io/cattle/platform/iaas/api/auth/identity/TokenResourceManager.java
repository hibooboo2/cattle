package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenHandler;
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

import com.netflix.config.DynamicStringProperty;

public class TokenResourceManager extends AbstractNoOpResourceManager {
    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString("api.auth.github.client.id");
    private static final DynamicStringProperty GITHUB_HOSTNAME = ArchaiusUtil.getString("api.github.domain");
    private List<TokenHandler> tokenHandlers;

    public List<TokenHandler> getTokenHandlers() {
        return tokenHandlers;
    }

    @Inject
    public void setTokenHandlers(List<TokenHandler> tokenHandlers) {
        this.tokenHandlers = tokenHandlers;
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
        for (TokenHandler tokenHandler : tokenHandlers) {
            try {
                token = tokenHandler.createToken(request);
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
        return new Token(null, null, null);
    }
}
