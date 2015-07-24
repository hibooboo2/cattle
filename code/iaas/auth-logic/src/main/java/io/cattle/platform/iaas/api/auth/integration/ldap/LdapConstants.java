package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class LdapConstants {

    public static final String NAME = "ldap";
    public static final String USER_SCOPE = NAME + "_user";
    public static final String GROUP_SCOPE = NAME + "_group";

    public static final String ACCESSMODE = "accessMode";
    public static final String ACCOUNT_ID = NAME + "_AccountId";
    public static final String CONFIG = NAME + "config";
    public static final String DOMAIN = "domain";
    public static final String LDAP_ACCESS_TOKEN = NAME + "AccessToken";
    public static final String LDAP_GROUPS = NAME + "_groups";
    public static final String LDAP_JWT = NAME + "Jwt";
    public static final String LDAP_USER_ID = USER_SCOPE + "_id";
    public static final String LOGIN_DOMAIN = "loginDomain";
    public static final String PORT = "port";
    public static final String SERVER = "server";
    public static final String SERVICE_ACCOUNT_PASSWORD = "serviceAccountPassword";
    public static final String SERVICE_ACCOUNT_USERNAME = "serviceAccountUsername";
    public static final String TLS = "tls";
    public static final String TOKEN = "token";
    public static final String USERNAME = NAME + "UserName";


    //Names for Settings in cattle.
    public static final String ACCESS_MODE_SETTING = "api.auth.ldap.access.mode";
    public static final String DOMAIN_SETTING = "api.auth.ldap.domain";
    public static final String LOGIN_DOMAIN_SETTING = "api.auth.ldap.login.domain";
    public static final String PORT_SETTING = "api.auth.ldap.port";
    public static final String SEARCH_FIELD_USER_SETTING = "api.auth.ldap.search.field.user";
    public static final String SERIVCE_ACCOUNT_USERNAME_SETTING = "api.auth.ldap.service.account.user";
    public static final String SERVER_SETTING = "api.auth.ldap.server";
    public static final String SERVICE_ACCOUNT_PASSWORD_SETTING = "api.auth.ldap.service.account.password";
    public static final String TLS_SETTING = "api.auth.ldap.tls";


    public static final String[] SCOPES = new String[]{USER_SCOPE, GROUP_SCOPE};

    public static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString(ACCESS_MODE_SETTING);
    public static final DynamicStringProperty LDAP_DOMAIN = ArchaiusUtil.getString(DOMAIN_SETTING);
    public static final DynamicStringProperty LDAP_LOGIN_DOMAIN = ArchaiusUtil.getString(LOGIN_DOMAIN_SETTING);
    public static final DynamicStringProperty LDAP_PORT = ArchaiusUtil.getString(PORT_SETTING);
    public static final DynamicStringProperty LDAP_SERVER = ArchaiusUtil.getString(SERVER_SETTING);
    public static final DynamicStringProperty SERVICEACCOUNT_PASSWORD = ArchaiusUtil.getString(SERVICE_ACCOUNT_PASSWORD_SETTING);
    public static final DynamicStringProperty SERVICEACCOUNT_USER = ArchaiusUtil.getString(SERIVCE_ACCOUNT_USERNAME_SETTING);

    public static final String TOKEN_CREATOR = NAME + "TokenCreator";
    public static final DynamicBooleanProperty TLS_ENABLED = ArchaiusUtil.getBoolean(TLS_SETTING);
    public static final DynamicStringProperty SEARCH_FIELD = ArchaiusUtil.getString(SEARCH_FIELD_USER_SETTING);
    public static final String SEARCH_FIELD_USER = "searchFieldUser";
    public static final String MANAGER = NAME + "Manager";


    //Should these be configurable settings?
    public static final String NAME_FIELD_USER = "name";
    public static final String NAME_FIELD_GROUP = "name";
    public static final String MEMBER_OF = "memberOf";
    public static final String OBJECT_CLASS = "objectClass";
    public static final String OBJECT_TYPE_GROUP = "group";
    public static final String OBJECT_TYPE_USER = "person";
    public static final String SEARCH_FIELD_GROUP = "sAMAccountName";
    public static final String DN = "distinguishedname";

}
