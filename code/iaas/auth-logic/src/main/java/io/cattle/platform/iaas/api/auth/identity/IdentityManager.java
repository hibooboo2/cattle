package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentitySearchProvider;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

public class IdentityManager extends AbstractNoOpResourceManager {

    private Map<String, IdentitySearchProvider> identitySearchProviders;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{Identity.class};
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {

        if (criteria.get("id") != null) {
            return Collections.singletonList(getIdentity((String) criteria.get("id")));
        }
        if (criteria.containsKey("name")) {
            Condition search = ((List<Condition>) criteria.get("name")).get(0);
            return searchIdentites((String)search.getValue());
        }
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        return refreshIdentities(policy.getIdentities());
    }

    /**
     * This method is used to update the cached identites from the policy, which are generated
     * from the users current token, which is only required to be updated once every
     * 16 hours.
     *
     * @param identities Set of identities from Policy.
     * @return Returns a new set of identities after using the api to call out to external
     * sources and update to the newest information. This request is expensive as it will
     * make N requests one for each Passed in identity.
     */
    private List<Identity> refreshIdentities(Set<Identity> identities) {
        List<Identity> identitiesToReturn = new ArrayList<>();
        for (Identity identity : identities) {
            Identity newIdentity = getIdentity(identity.getId());
            authorize(newIdentity);
            identitiesToReturn.add(newIdentity);
        }
        return identitiesToReturn;
    }

    private Identity getIdentity(String id) {
        String[] split = id.split(":", 2);
        if (split.length != 2) {
            return null;
        }
        Identity identity = null;
        for (IdentitySearchProvider identitySearchProvider : identitySearchProviders.values()) {
            if (identitySearchProvider.scopesProvided().contains(split[0])) {
                identity = identitySearchProvider.getIdentity(split[1], split[0]);
            }
            if (identity != null) {
                break;
            }
        }
        return identity;
    }

    private List<Identity> searchIdentites(String name) {
        Set<Identity> identities = new HashSet<>();
        for (IdentitySearchProvider identitySearchProvider : identitySearchProviders.values()) {
            identities.addAll(identitySearchProvider.searchIdentities(name));
        }
        for (Identity identity : identities) {
            authorize(identity);
        }
        return new ArrayList<>(identities);
    }

    @Inject
    public void setIdentitySearchProviders(Map<String, IdentitySearchProvider> identitySearchProviders) {
        this.identitySearchProviders = identitySearchProviders;
    }
}
