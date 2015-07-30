package io.cattle.platform.iaas.api.auth.integration.ldap;

import static javax.naming.directory.SearchControls.*;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentitySearchProvider;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.SSLSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LdapIdentitySearchProvider extends LdapConfigurable implements IdentitySearchProvider {

    private static final Log logger = LogFactory.getLog(LdapIdentitySearchProvider.class);
    @Inject
    LdapUtils ldapUtils;

    public List<Identity> searchIdentities(String name, boolean exactMatch) {
        if (!isConfigured()){
            return new ArrayList<>();
        }
        List<Identity> identities = new ArrayList<>();
        for (String scope : scopesProvided()) {
            identities.addAll(searchIdentities(name, scope, exactMatch));
        }
        return identities;
    }

    @Override
    public List<Identity> searchIdentities(String name, String scope, boolean exactMatch) {
        //TODO:Implement Exact match vs none exact match.
        if (!isConfigured()){
            return new ArrayList<>();
        }
        switch (scope) {
            case LdapConstants.USER_SCOPE:
                return searchUser(name, exactMatch);
            case LdapConstants.GROUP_SCOPE:
                return searchGroup(name, exactMatch);
            default:
                return new ArrayList<>();
        }
    }

    @Override
    public Identity getIdentity(String distinguishedName, String scope) {
        if (!isConfigured()){
            return null;
        }
        switch (scope) {
            case LdapConstants.USER_SCOPE:
                return getUser(distinguishedName);
            case LdapConstants.GROUP_SCOPE:
                return getGroup(distinguishedName);
            default:
                return null;
        }
    }

    @Override
    public List<String> scopesProvided() {
        if (!isConfigured()){
            return new ArrayList<>();
        }
        return Arrays.asList(LdapConstants.SCOPES);
    }

    @Override
    public String getName() {
        return LdapConstants.NAME;
    }

    private LdapContext login(String username, String password) {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, password);
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        LdapContext userContext;

        try {
            String url = "ldap://" + LdapConstants.LDAP_SERVER.get() + ':' + LdapConstants.LDAP_PORT.get() + '/';
            props.put(Context.PROVIDER_URL, url);
            userContext = new InitialLdapContext(props, null);
            StartTlsResponse tlsResponse;
            SSLSession sslSession;
            if (LdapConstants.TLS_ENABLED.get()){
                tlsResponse =(StartTlsResponse) userContext.extendedOperation(new StartTlsRequest());
                sslSession = tlsResponse.negotiate();
            }
            return userContext;
        } catch (NamingException e) {
            logger.error("Failed to bind to LDAP", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start ssl", e);
        }
    }

    private Attributes userRecord(LdapContext context, String scope, String name) {
        SearchControls controls = new SearchControls();
        name = getSamName(name);
        controls.setSearchScope(SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> results;
        try {
            String query = "(" + LdapConstants.SEARCH_FIELD.get() + '=' + name + ")";
            results = context.search(scope, query, controls);
        } catch (NamingException e) {
            logger.error("Failed to search: " + name, e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapConfig", "Organizational Unit not found.", null);
        }
        try {
            if (!results.hasMore()) {
                logger.info("Cannot locate user information for " + name);
                return null;
            }
        } catch (NamingException e) {
            logger.error("Common name: " + name + " is not valid.", e);
            return null;
        }
        SearchResult result;
        try {
            result = results.next();
            if (results.hasMoreElements()) {
                logger.error("More than one result.");
                return null;
            }
            if (!hasPermission(result.getAttributes())){
                return null;
            }
        } catch (NamingException e) {
            logger.error("No results. when searching. " + name);
            return null;
        }
        return result.getAttributes();
    }

    private Set<Identity> getIdentities(Attributes userAttributes) {
        Set<Identity> identities = new HashSet<>();
        Attribute memberOf = userAttributes.get(LdapConstants.MEMBER_OF);
        try {
            if (!isType(userAttributes, LdapConstants.OBJECT_TYPE_USER))
            {
                return identities;
            }
            identities.add(getUser((String) userAttributes.get(LdapConstants.DN).get()));
            if (memberOf != null) {// null if this user belongs to no group at all
                for (int i = 0; i < memberOf.size(); i++) {
                    identities.add(getGroup(memberOf.get(i).toString()));
                }
            }
            return identities;
        } catch (NamingException e) {
            logger.error("Exceptions on groups.", e);
            return new HashSet<>();
        }
    }

    public Set<Identity> getIdentities(String username, String password) {
        if (!isConfigured()) {
            return new HashSet<>();
        }
        LdapContext userContext;
        try {
            userContext = login(getUserExternalId(username), password);
        } catch (RuntimeException e) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        Attributes userAttributes = userRecord(userContext, LdapConstants.LDAP_DOMAIN.get(), username);
        if (userAttributes == null){
            return new HashSet<>();
        }
        Set<Identity> identities = getIdentities(userAttributes);
        try {
            userContext.close();
        } catch (NamingException e) {
            logger.error("Failed to close userContext.", e);
        }
        return identities;
    }

    public String getUserExternalId(String username) {
        if (!isConfigured()) {
            return null;
        }
        if (username.contains("\\")) {
            return username;
        } else {
            return LdapConstants.LDAP_LOGIN_DOMAIN.get() + '\\' +username;
        }
    }

    public String getSamName(String username) {
        if (!isConfigured()) {
            return null;
        }
        if (username.contains("\\")) {
            return username.split("\\\\", 2)[1];
        } else {
            return username;
        }
    }

    private List<Identity> searchGroup(String name, boolean exactMatch) {
        //TODO: Implement group search.
        return new ArrayList<>();
    }

    private List<Identity> searchUser(String name, boolean exactMatch) {
        LdapContext context = getServiceContext();
        String query = "(" + LdapConstants.SEARCH_FIELD.get() + '=' + name + ")";
        return searchLdap(context, LdapConstants.LDAP_DOMAIN.get(), name, query);
    }

    private Identity getUser(String distinguishedName) {
        try {
            LdapContext context = getServiceContext();
            if (context == null) {
                return null;
            }
            Attributes search = context.getAttributes(new LdapName(distinguishedName));
            if (!isType(search, LdapConstants.OBJECT_TYPE_USER) && !hasPermission(search)){
                return null;
            }
            String accountName = (String) search.get(LdapConstants.NAME_FIELD_USER).get();
            String externalId = (String) search.get(LdapConstants.DN).get();
            return new Identity(LdapConstants.USER_SCOPE, externalId, accountName);
        } catch (NamingException e) {
            logger.error("Failed to get user.", e);
            return null;
        }
    }

    private boolean isType(Attributes search, String type) throws NamingException {
        NamingEnumeration<?> objectClass = search.get(LdapConstants.OBJECT_CLASS).getAll();
        boolean isType = false;
        while (objectClass.hasMoreElements()) {
            Object object = objectClass.next();
            if ((object.toString()).equalsIgnoreCase(type)){
                isType = true;
            }
        }
        return isType;
    }

    private Identity getGroup(String distinguishedName) {
        try {
            LdapContext context = getServiceContext();
            if (context == null){
                return null;
            }
            Attributes search = context.getAttributes(new LdapName(distinguishedName));
            if (!isType(search, LdapConstants.OBJECT_TYPE_GROUP)){
                return null;
            }
            String accountName = (String) search.get(LdapConstants.NAME_FIELD_GROUP).get();
            String externalId = (String) search.get(LdapConstants.DN).get();
            return new Identity(LdapConstants.GROUP_SCOPE, externalId, accountName);
        } catch (NamingException e) {
            logger.error("Failed to get group.", e);
            return null;
        }

    }

    private LdapContext getServiceContext() {
        if (isConfigured()) {
            return login(getUserExternalId(LdapConstants.SERVICEACCOUNT_USER.get()), LdapConstants.SERVICEACCOUNT_PASSWORD.get());
        } else {
            return null;
        }
    }

    private List<Identity> searchLdap(LdapContext context, String ldapScope, String name, String query) {
        List<Identity> identities = new ArrayList<>();
        SearchControls controls = new SearchControls();
        name = getSamName(name);
        controls.setSearchScope(SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> results;
        try {
            results = context.search(ldapScope, query, controls);
        } catch (NamingException e) {
            logger.error("Failed to search: " + name, e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapConfig", "Organizational Unit not found.", null);
        }
        try {
            if (!results.hasMore()) {
                return identities;
            }
        } catch (NamingException e) {
            return identities;
        }
        try {
            while (results.hasMore()){
                identities.add(resultToIdentity(results.next().getAttributes()));
            }
        } catch (NamingException e) {
            logger.error("No results. when searching. " + name);
            throw new RuntimeException(e);
        }
        return identities;
    }

    private Identity resultToIdentity(Attributes search){
        try {
            if (!hasPermission(search)){
                return null;
            }
            String kind;
            String accountName;
            String externalId;
            if (isType(search, LdapConstants.OBJECT_TYPE_USER)){
                kind = LdapConstants.USER_SCOPE;
                accountName = (String) search.get(LdapConstants.NAME_FIELD_USER).get();
                externalId = (String) search.get(LdapConstants.DN).get();
            } else if (isType(search, LdapConstants.OBJECT_TYPE_GROUP)) {
                kind = LdapConstants.GROUP_SCOPE;
                accountName = (String) search.get(LdapConstants.NAME_FIELD_GROUP).get();
                externalId = (String) search.get(LdapConstants.DN).get();
            } else {
                return null;
            }
            return new Identity(kind, externalId, accountName);
        } catch (NamingException e) {
            return null;
        }
    }

    private boolean hasPermission(Attributes attributes){
        int permission;
        try {
            if (!isType(attributes, LdapConstants.OBJECT_TYPE_USER)){
                return true;
            }
            permission = Integer.parseInt(attributes.get(LdapConstants.USER_ACCOUNT_CONTROL_FIELD).get()
                    .toString());
        } catch (NamingException e) {
            logger.error("Failed to get USER_ACCOUNT_CONTROL_FIELD.", e);
            return false;
        }
        permission = permission & LdapConstants.HAS_ACCESS_BIT;
        return permission != LdapConstants.HAS_ACCESS_BIT;
    }
}
