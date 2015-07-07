package io.cattle.platform.iaas.api.auth.integrations.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.AuthUtils;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integrations.github.resource.Token;
import io.cattle.platform.iaas.api.auth.interfaces.TokenHandler;
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
        if (!StringUtils.equals(AuthUtils.TOKEN, request.getType())) {
            return null;
        }
        try {
            return getToken(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Token getToken(ApiRequest request) throws IOException {
        Token token = null;
        List<ClientVisibleException> exceptions = new ArrayList<>();
        for (TokenHandler tokenHandler : tokenHandlers) {
            try {
                token = tokenHandler.getToken(request);
            } catch (ClientVisibleException e) {
                exceptions.add(e);
            }
            if (token != null) {
                break;
            }
        }
        if (token == null && !exceptions.isEmpty()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "AuthorizationProvider", "Either No Authorzation Provider configured or " +
                    "invalid code.",
                    null);
        }
        return token;
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return new Token(SecurityConstants.SECURITY.get(), GITHUB_CLIENT_ID.get(), GITHUB_HOSTNAME.get());
    }
}
