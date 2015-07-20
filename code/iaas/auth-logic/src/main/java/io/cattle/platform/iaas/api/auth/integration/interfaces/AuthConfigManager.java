package io.cattle.platform.iaas.api.auth.integration.interfaces;

import java.util.Map;

public interface AuthConfigManager extends Configurable{

    AuthConfig getCurrentConfig(Map<String, Object> config);

    AuthConfig updateCurrentConfig(Map<String, Object> config);
}
