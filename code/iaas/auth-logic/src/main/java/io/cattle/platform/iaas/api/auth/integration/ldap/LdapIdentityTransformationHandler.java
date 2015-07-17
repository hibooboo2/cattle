package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityTransformationHandler;

import java.util.Set;
import javax.inject.Inject;

public class LdapIdentityTransformationHandler implements IdentityTransformationHandler {

    @Inject
    LdapIdentitySearchProvider ldapIdentitySearchProvider;

    @Inject
    LdapUtils ldapUtils;

    @Override
    public Identity transform(Identity identity) {
        return identity;
    }

    @Override
    public Identity untransform(Identity identity) {
        return identity;
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        return ldapUtils.getIdentities();
    }
}
