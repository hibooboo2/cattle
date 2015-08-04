package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;

import org.apache.commons.lang3.StringUtils;

public abstract class LdapConfigurable implements Configurable {

    @Override
    public boolean isConfigured() {
        boolean allProps = StringUtils.isNotBlank(LdapConstants.USER_LOGIN_FIELD.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_LOGIN_DOMAIN.get()) &&
                StringUtils.isNotBlank(String.valueOf(LdapConstants.LDAP_PORT.get())) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_SERVER.get()) &&
                StringUtils.isNotBlank(LdapConstants.ACCESS_MODE.get()) &&
                StringUtils.isNotBlank(LdapConstants.SERVICEACCOUNT_USER.get()) &&
                StringUtils.isNotBlank(LdapConstants.SERVICEACCOUNT_PASSWORD.get()) &&
                StringUtils.isNotBlank(LdapConstants.USER_SEARCH_FIELD.get()) &&
                StringUtils.isNotBlank(LdapConstants.USER_OBJECT_CLASS.get()) &&
                StringUtils.isNotBlank(String.valueOf(LdapConstants.USER_ENABLED_MASK_BIT_SETTING.get())) &&
                StringUtils.isNotBlank(LdapConstants.USER_ENABLED_ATTRIBUTE.get()) &&
                StringUtils.isNotBlank(LdapConstants.USER_NAME_FIELD.get()) &&
                StringUtils.isNotBlank(LdapConstants.GROUP_SEARCH_FIELD.get()) &&
                StringUtils.isNotBlank(LdapConstants.GROUP_OBJECT_CLASS.get()) &&
                StringUtils.isNotBlank(LdapConstants.GROUP_NAME_FIELD.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_DOMAIN.get());
        if (SecurityConstants.SECURITY.get()){
            return         StringUtils.equalsIgnoreCase(SecurityConstants.AUTHPROVIDER.get(), LdapConstants.CONFIG) &&
                    allProps;
        } else {
            return allProps;
        }
    }
}
