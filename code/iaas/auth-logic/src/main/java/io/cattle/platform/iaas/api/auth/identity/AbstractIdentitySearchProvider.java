package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentitySearchProvider;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractIdentitySearchProvider implements IdentitySearchProvider {

    public List<Identity> searchIdentities(String name) {
        List<Identity> identities = new ArrayList<>();
        for (String scope : scopesProvided()) {
            identities.addAll(searchIdentities(name, scope));
        }
        return identities;
    }

    public abstract List<Identity> searchIdentities(String name, String scope);

    public abstract Identity getIdentity(String id, String scope);

    public abstract List<String> scopesProvided();
}
