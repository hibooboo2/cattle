package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.api.auth.Identity;

import java.util.List;

public interface IdentitySearchProvider extends Configurable {

    List<Identity> searchIdentities(String name);

    List<Identity> searchIdentities(String name, String scope);

    Identity getIdentity(String id, String scope);

    List<String> scopesProvided();
}
