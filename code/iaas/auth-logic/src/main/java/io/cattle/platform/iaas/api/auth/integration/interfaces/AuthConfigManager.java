package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.util.type.Named;

import java.util.Map;

public interface AuthConfigManager extends Named{

    AuthConfig getCurrentConfig(Map<String, Object> config);

    AuthConfig updateCurrentConfig(Map<String, Object> config);
}
