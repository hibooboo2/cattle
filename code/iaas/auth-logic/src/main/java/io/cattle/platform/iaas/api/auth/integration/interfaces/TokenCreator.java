package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.iaas.api.auth.identity.Token;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

public interface TokenCreator extends Configurable{

    Token createToken(ApiRequest request) throws IOException;

}
