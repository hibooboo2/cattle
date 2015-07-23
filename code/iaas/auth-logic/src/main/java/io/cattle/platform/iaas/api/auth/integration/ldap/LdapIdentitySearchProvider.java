package io.cattle.platform.iaas.api.auth.integration.ldap;

import static javax.naming.directory.SearchControls.*;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.identity.AbstractIdentitySearchProvider;

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
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.jndi.ldap.LdapCtxFactory;

public class LdapIdentitySearchProvider extends AbstractIdentitySearchProvider {

    private static final Log logger = LogFactory.getLog(LdapIdentitySearchProvider.class);
    @Inject
    LdapUtils ldapUtils;

    @Override
    public boolean isConfigured() {
        return StringUtils.isNotBlank(LdapConstants.LDAP_SERVER.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_PORT.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_DOMAIN.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_LOGIN_DOMAIN.get()) &&
                StringUtils.isNotBlank(LdapConstants.SERVICEACCOUNT_USER.get()) &&
                StringUtils.isNotBlank(LdapConstants.SERVICEACCOUNT_PASSWORD.get());
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
        LdapContext userContext;

        try {
            String scheme = LdapConstants.TLS_ENABLED.get() ? "ldaps://" : "ldap://";
            String url = scheme + LdapConstants.LDAP_SERVER.get() + ':' + LdapConstants.LDAP_PORT.get() + '/';
            userContext = (LdapContext) LdapCtxFactory.getLdapCtxInstance( url, props);
            return userContext;
        } catch (NamingException e) {
            logger.error("Failed to bind to LDAP / get account information: " + e);
            throw new RuntimeException(e);
        }
    }

    private SearchResult userRecord(LdapContext context, String domain, String name) {
        SearchControls controls = new SearchControls();
        name = getSAMname(name);
        controls.setSearchScope(SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> renum;
        try {
            String query = '(' + LdapConstants.SEARCH_FIELD.get() + '=' + name + ')';
            renum = context.search(toDC(domain), query, controls);
        } catch (NamingException e) {
            logger.error("Failed to search: " + name, e);
            return null;
        }
        try {
            if (!renum.hasMore()) {
                logger.info("Cannot locate user information for " + name);
                return null;
            }
        } catch (NamingException e) {
            logger.error("Common name: " + name + " is not valid.", e);
            return null;
        }
        SearchResult result;
        try {
            result = renum.next();
            if (renum.hasMoreElements()) {
                logger.error("More than one result.");
                return null;
            }
        } catch (NamingException e) {
            logger.error("No results. when searching. " + name);
            return null;
        }
        return result;
    }

    private Set<Identity> getIdentities(SearchResult result, LdapContext context) {
        Set<Identity> identities = new HashSet<>();
        if (result == null) {
            return identities;
        }
        Attributes userAttributes = result.getAttributes();
        Attribute memberOf = result.getAttributes().get("memberOf");
        try {
            identities.add(getUser((String) userAttributes.get(LdapConstants.DN).get()));
            if (memberOf != null) {// null if this user belongs to no group at all
                for (int i = 0; i < memberOf.size(); i++) {
                    Attributes attributes = context.getAttributes(memberOf.get(i).toString(), new String[]{"CN"});
                    Attribute commonName = attributes.get("CN");
                    identities.add(new Identity(LdapConstants.GROUP_SCOPE, memberOf.get(i).toString(), commonName.get().toString()));
                }
            }
            return identities;
        } catch (NamingException e) {
            logger.error("Exceptions on groups.", e);
            return new HashSet<>();
        }
    }

    private String toDC(String domainName) {
        StringBuilder buf = new StringBuilder();
        for (String token : domainName.split("\\.")) {
            if (token.length() == 0) continue;   // defensive check
            if (buf.length() > 0) buf.append(",");
            buf.append("DC=").append(token);
        }
        return buf.toString();
    }

    public Set<Identity> getIdentities(String username, String password) {
        if (!isConfigured()) {
            return new HashSet<>();
        }
        LdapContext userContext = login(getUserExternalId(username), password);
        Set<Identity> identities = getIdentities(userRecord(userContext, LdapConstants.LDAP_DOMAIN.get(), username), userContext);
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

    public String getSAMname(String username) {
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
        try {
            if (!isConfigured()){
                return new ArrayList<>();
            }
            LdapContext context = getServiceContext();
            SearchResult result = userRecord(context, LdapConstants.LDAP_DOMAIN.get(), name);
            if (result == null) {
                return new ArrayList<>();
            }
            Attributes attributes = result.getAttributes();
            String accountName = (String) attributes.get("name").get();
            String externalId = (String) attributes.get(LdapConstants.DN).get();
            Identity identity = new Identity(LdapConstants.USER_SCOPE, externalId, accountName);
            List<Identity> identities = new ArrayList<>();
            identities.add(identity);
            return identities;
        } catch (NamingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Identity getUser(String distinguishedName) {
        try {
            LdapContext context = getServiceContext();
            Attributes search = context.getAttributes(new LdapName(distinguishedName));
            String accountName = (String) search.get("cn").get();
            String externalId = (String) search.get(LdapConstants.DN).get();
            return new Identity(LdapConstants.USER_SCOPE, externalId, accountName);
        } catch (NamingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Identity getGroup(String distinguishedName) {
        try {
            LdapContext context = getServiceContext();
            Attributes search = context.getAttributes(new LdapName(distinguishedName));
            String accountName = (String) search.get("name").get();
            String externalId = (String) search.get(LdapConstants.DN).get();
            return new Identity(LdapConstants.GROUP_SCOPE, externalId, accountName);
        } catch (NamingException e) {
            e.printStackTrace();
            return null;
        }

    }

    public LdapContext getServiceContext() {
        if (isConfigured()) {
            return login(getUserExternalId(LdapConstants.SERVICEACCOUNT_USER.get()), LdapConstants.SERVICEACCOUNT_PASSWORD.get());
        } else {
            return null;
        }
    }
}
