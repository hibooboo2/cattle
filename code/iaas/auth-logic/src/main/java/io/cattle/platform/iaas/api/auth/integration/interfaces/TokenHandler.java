package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.iaas.api.auth.integration.github.resource.Token;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

public interface TokenHandler {

    Token createToken(ApiRequest request) throws IOException;

}
