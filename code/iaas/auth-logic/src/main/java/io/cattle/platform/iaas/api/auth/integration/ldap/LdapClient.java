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
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.jndi.ldap.LdapCtxFactory;

public class LdapClient extends AbstractIdentitySearchProvider {

    private static final Log logger = LogFactory.getLog(LdapClient.class);
    @Inject
    LdapUtils ldapUtils;

    private DirContext login(String username, String password) {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.SECURITY_PRINCIPAL, username);
        props.put(Context.SECURITY_CREDENTIALS, password);
        DirContext context;

        try {
            context = LdapCtxFactory.getLdapCtxInstance("ldap://" + LdapConstants.LDAP_SERVER.get() + ':' + LdapConstants.LDAP_PORT.get() + '/', props);
            return context;
        } catch (NamingException e) {
            logger.error("Failed to bind to LDAP / get account information: " + e);
            throw new RuntimeException(e);
        }
    }

    private SearchResult userRecord(DirContext context, String domain, String samName) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> renum;
        try {
            renum = context.search(toDC(domain), "(sAMAccountName=" + samName + ")", controls);
        } catch (NamingException e) {
            logger.error("Failed to search: " + samName, e);
            return null;
        }
        try {
            if (!renum.hasMore()) {
                logger.info("Cannot locate user information for " + samName);
                return null;
            }
        } catch (NamingException e) {
            logger.error("Common name: " + samName + " is not valid.", e);
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
            logger.error("No results. when searching. " + samName);
            return null;
        }
        return result;
    }

    private Set<Identity> getGroups(SearchResult result, DirContext context) {
        Set<Identity> groups = new HashSet<>();
        if (result == null) {
            return groups;
        }
        Attribute memberOf = result.getAttributes().get("memberOf");
        try {
            if (memberOf != null) {// null if this user belongs to no group at all
                for (int i = 0; i < memberOf.size(); i++) {
                    Attributes attributes = context.getAttributes(memberOf.get(i).toString(), new String[]{"CN"});
                    Attribute attribute = attributes.get("CN");
                    groups.add(new Identity(LdapConstants.GROUP_SCOPE, memberOf.get(i).toString(), attribute.get().toString()));
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
        DirContext context = login(getUserExternalId(username), password);
        SearchResult result = userRecord(context, LdapConstants.LDAP_DOMAIN.get(), username);
        Set<Identity> groups = getGroups(result, context);
        try {
            context.close();
        } catch (NamingException e) {
            logger.error("Failed to close context.", e);
        }
        return groups;
    }

    public String getUserExternalId(String username) {
        if (!isConfigured()) {
            return null;
        }
        if (username.startsWith(LdapConstants.LDAP_LOGIN_DOMAIN.get() + '\\')) {
            return username;
        } else {
            return LdapConstants.LDAP_LOGIN_DOMAIN.get() + '\\' + username;
        }
    }

    public boolean isConfigured() {
        return StringUtils.isNotBlank(LdapConstants.LDAP_SERVER.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_PORT.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_DOMAIN.get()) &&
                StringUtils.isNotBlank(LdapConstants.LDAP_LOGIN_DOMAIN.get());
    }

    @Override
    public List<Identity> searchIdentities(String name, String scope) {
        String accessToken = ldapUtils.getAccessToken();
        String[] split = accessToken.split(":", 2);
        Set<Identity> identities = getIdentities(split[0], split[1]);
        List<Identity> identitiesToReturn = new ArrayList<>();
        switch (scope) {
            case LdapConstants.USER_SCOPE:
                addIds(LdapConstants.USER_SCOPE, identities, identitiesToReturn);
                break;
            case LdapConstants.GROUP_SCOPE:
                addIds(LdapConstants.USER_SCOPE, identities, identitiesToReturn);
                break;
            default:
                break;
        }
        return identitiesToReturn;
    }

    private void addIds(String scope, Set<Identity> identities, List<Identity> identitiesToReturn) {
        for (Identity identity : identities) {
            if (identity.getKind().equalsIgnoreCase(scope)) {
                identitiesToReturn.add(identity);
            }
        }
    }

    @Override
    public Identity getIdentity(String id, String scope) {
        switch (scope) {
            case LdapConstants.USER_SCOPE:
                return getGroup(id);
            case LdapConstants.GROUP_SCOPE:
                return getGroup(id);
            default:
                return null;
        }
    }

    private Identity getGroup(String id) {
        try {
            String[] split = ldapUtils.getAccessToken().split(":", 2);
            DirContext context = login(getUserExternalId(split[0]), split[1]);
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

    private Identity getUser(String id) {
        return null;
    }

    @Override
    public List<String> scopesProvided() {
        return Arrays.asList(LdapConstants.SCOPES);
    }

    @Override
    public String getName() {
        return LdapConstants.NAME;
    }
}
