package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class SecurityConstants {

    public static final String ENABLED = "enabled";
    public static final String SECURITY_SETTING = "api.security.enabled";
    public static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean(SECURITY_SETTING);
    public static final String AUTH_PROVIDER_SETTING = "api.auth.provider.configured";
    public static final DynamicStringProperty AUTHPROVIDER = ArchaiusUtil.getString(AUTH_PROVIDER_SETTING);

    public static final String NO_PROVIDER = "none";
}
