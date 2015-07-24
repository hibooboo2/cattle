package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;

import org.apache.commons.lang3.StringUtils;

public abstract class LdapConfigurable implements Configurable {

    @Override
    public boolean isConfigured() {
return         StringUtils.equalsIgnoreCase(SecurityConstants.AUTHPROVIDER.get(), LdapConstants.CONFIG) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_SERVER.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_PORT.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_DOMAIN.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_LOGIN_DOMAIN.get()) &&
                StringUtils.isNotBlank(LdapConstants.SERVICEACCOUNT_USER.get()) &&
                StringUtils.isNotBlank(LdapConstants.SERVICEACCOUNT_PASSWORD.get());
    }
}
