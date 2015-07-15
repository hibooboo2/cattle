package io.cattle.platform.iaas.api.auth.interfaces;

import io.cattle.platform.iaas.api.auth.integrations.github.resource.Token;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

public interface TokenHandler {

    public Token getToken(ApiRequest request) throws IOException;

}
