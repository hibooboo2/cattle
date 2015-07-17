package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicStringProperty;

public class LdapConstants {

    public static final String NAME = "ldap";

    public static final String ACCESS_MODE_SETTING = "api.auth.ldap.access.mode";
    public static final String ACCESSMODE = "accessMode";
    public static final String ACCOUNT_ID = NAME + "_AccountId";
    public static final String CONFIG = NAME + "config";
    public static final String DOMAIN = "domain";
    public static final String DOMAIN_SETTING = "api.auth.ldap.domain";
    public static final String GROUP_SCOPE = NAME + "_group";
    public static final String LDAP_ACCESS_TOKEN = NAME + "AccessToken";
    public static final String LDAP_GROUPS = NAME + "_groups";
    public static final String LDAP_JWT = NAME + "Jwt";
    public static final String LOGIN_DOMAIN = "loginDomain";
    public static final String LOGIN_DOMAIN_SETTING = "api.auth.ldap.login.domain";
    public static final String PORT = "port";
    public static final String PORT_SETTING = "api.auth.ldap.port";
    public static final String SERIVCEACCOUNTUSERNAME_SETTING = "api.auth.ldap.service.account.user";
    public static final String SERVER = "server";
    public static final String SERVER_SETTING = "api.auth.ldap.server";
    public static final String SERVICEACCOUNTPASSWORD = "serviceAccountPassword";
    public static final String SERVICEACCOUNTPASSWORD_SETTING = "api.auth.ldap.service.account.password";
    public static final String SERVICEACCOUNTUSERNAME = "serviceAccountUsername";
    public static final String TOKEN = "token";
    public static final String USER_SCOPE = NAME + "_user";
    public static final String USERNAME = NAME + "UserName";
    public static final String LDAPUSERID = USER_SCOPE + "_id";



    public static final String[] SCOPES = new String[]{USER_SCOPE, GROUP_SCOPE};

    public static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString(ACCESS_MODE_SETTING);
    public static final DynamicStringProperty LDAP_DOMAIN = ArchaiusUtil.getString(DOMAIN_SETTING);
    public static final DynamicStringProperty LDAP_LOGIN_DOMAIN = ArchaiusUtil.getString(LOGIN_DOMAIN_SETTING);
    public static final DynamicStringProperty LDAP_PORT = ArchaiusUtil.getString(PORT_SETTING);
    public static final DynamicStringProperty LDAP_SERVER = ArchaiusUtil.getString(SERVER_SETTING);
    public static final DynamicStringProperty SERVICEACCOUNT_PASSWORD = ArchaiusUtil.getString(SERVICEACCOUNTPASSWORD_SETTING);
    public static final DynamicStringProperty SERVICEACCOUNT_USER = ArchaiusUtil.getString(SERIVCEACCOUNTUSERNAME_SETTING);

    public static final String TOKEN_CREATOR = NAME + "TokenCreator";
}