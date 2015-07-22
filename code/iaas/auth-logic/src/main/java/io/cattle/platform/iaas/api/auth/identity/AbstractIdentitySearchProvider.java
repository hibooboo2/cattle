package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentitySearchProvider;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractIdentitySearchProvider implements IdentitySearchProvider {

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

    /**
     * Used as a general entry point to search based on scope. This will genearally be implemented
     * as a switch statement that will in turn call internal private methods that the implementing class
     * can use to search the specified scopes provided.
     *
     * @param name the value to search for.
     * @param scope the scope to search in.
     * @return {@link List} of identities found based on search.
     */
    public abstract List<Identity> searchIdentities(String name, String scope, boolean exactMatch);

    public abstract Identity getIdentity(String id, String scope);

    public abstract List<String> scopesProvided();

}
