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
import javax.naming.NameClassPair;
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

    private LdapContext login(String username, String password) {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, password);
        LdapContext context;

        try {
            context = (LdapContext) LdapCtxFactory.getLdapCtxInstance("ldap://" + LdapConstants.LDAP_SERVER.get() + ':' + LdapConstants.LDAP_PORT.get() + '/', props);
            return context;
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
            renum = context.search(toDC(domain), "(sAMAccountName=" + name + ")", controls);
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

    private Set<Identity> getGroups(SearchResult result, LdapContext context) {
        Set<Identity> groups = new HashSet<>();
        if (result == null) {
            return groups;
        }
        String cn = result.getNameInNamespace();
        Attribute memberOf = result.getAttributes().get("memberOf");
        try {
            if (memberOf != null) {// null if this user belongs to no group at all
                for (int i = 0; i < memberOf.size(); i++) {
                    Attributes attributes = context.getAttributes(memberOf.get(i).toString(), new String[]{"CN"});
                    Attribute commonName = attributes.get("CN");
                    Attribute samName = attributes.get("CN");
                    groups.add(new Identity(LdapConstants.GROUP_SCOPE, memberOf.get(i).toString(), commonName.get().toString()));
                }
            }
        } catch (NamingException e) {
            logger.error("Exceptions on groups.", e);
            return groups;
        }
        return groups;
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
        LdapContext context = login(getUserExternalId(username), password);
        SearchResult result = userRecord(context, LdapConstants.LDAP_DOMAIN.get(), username);
        Set<Identity> identities = getGroups(result, context);
        identities.add(new Identity(LdapConstants.USER_SCOPE, getUserExternalId(username), username));
        try {
            context.close();
        } catch (NamingException e) {
            logger.error("Failed to close context.", e);
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
    public List<Identity> searchIdentities(String name, String scope) {
        if (!isConfigured()){
            return new ArrayList<>();
        }
        switch (scope) {
            case LdapConstants.USER_SCOPE:
                return searchUser(name);
            case LdapConstants.GROUP_SCOPE:
                return searchGroup(name);
            default:
                return new ArrayList<>();
        }
    }

    private List<Identity> searchGroup(String name) {
        return new ArrayList<>();
    }

    private List<Identity> searchUser(String name) {
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
            String externalId = (String) attributes.get("distinguishedname").get();
            Identity identity = new Identity(LdapConstants.USER_SCOPE, externalId, accountName);
            List<Identity> identities = new ArrayList<>();
            identities.add(identity);
            return identities;
        } catch (NamingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Identity getIdentity(String id, String scope) {
        switch (scope) {
            case LdapConstants.USER_SCOPE:
                return getUser(id);
            case LdapConstants.GROUP_SCOPE:
                return getGroup(id);
            default:
                return null;
        }
    }

    private Identity getGroup(String id) {
        return new Identity(LdapConstants.GROUP_SCOPE, id, "NOT YET MADE PHANTOM>!>!?");
    }

    private Identity getUser(String id) {
        try {
            LdapContext context = getServiceContext();
            context = (LdapContext) context.lookup(new LdapName(id));
            NamingEnumeration<NameClassPair> x = context.list(id);
            SearchResult result = userRecord(context, LdapConstants.LDAP_DOMAIN.get(), id);
            Attributes attributes = result.getAttributes();
            String accountName = (String) attributes.get("displayname").get();
            String externalId = (String) attributes.get("distinguishedname").get();
            Identity identity = new Identity(LdapConstants.USER_SCOPE, externalId, accountName);
            return identity;
        } catch (NamingException e) {
            e.printStackTrace();
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

    public LdapContext getServiceContext() {
        if (isConfigured()) {
            return login(getUserExternalId(LdapConstants.SERVICEACCOUNT_USER.get()), LdapConstants.SERVICEACCOUNT_PASSWORD.get());
        } else {
            return null;
        }
    }
}
