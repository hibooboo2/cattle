package io.cattle.platform.iaas.api.auth.providers.ldap;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicStringProperty;

/**
 * Created by wizardofmath on 7/9/15.
 */
public class LdapConstants {
    public static final String GROUP_SCOPE = "LdapGroup";
    public static final String USER_SCOPE = "LdapUser";
    public static final String USERNAME = "LdapUserName";
    public static final String LDAP_GROUPS = "LdapGroups";
    public static final String LDAP_ACCESS_TOKEN = "LdapAccessToken";
    public static final String ACCOUNT_ID = "Ldap_AccountId";
    public static final String LDAP_JWT = "LdapJwt";
    public static final String TOKEN = "token";
    public static final String LDAPCONFIG = "ldapconfig";
    public static final String ACCESSMODE = "accessMode";


    public static final String DOMAIN = "domain";
    public static final String DOMAIN_SETTING = "api.auth.ldap.domain";
    public static final DynamicStringProperty LDAP_DOMAIN = ArchaiusUtil.getString(DOMAIN_SETTING);
    public static final String LOGIN_DOMAIN = "loginDomain";
    public static final String LOGIN_DOMAIN_SETTING = "api.auth.ldap.login.domain";
    public static final DynamicStringProperty LDAP_LOGIN_DOMAIN = ArchaiusUtil.getString(LOGIN_DOMAIN_SETTING);
    public static final String SERVER = "server";
    public static final String SERVER_SETTING = "api.auth.ldap.server";
    public static final DynamicStringProperty LDAP_SERVER = ArchaiusUtil.getString(SERVER_SETTING);
    public static final String PORT = "port";
    public static final String PORT_SETTING = "api.auth.ldap.port";
    public static final DynamicStringProperty LDAP_PORT = ArchaiusUtil.getString(PORT_SETTING);
    public static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString("api.auth.github.access.mode");

}
