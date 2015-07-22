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
        switch (identity.getKind()){
            case LdapConstants.USER_SCOPE:
                break;
            case LdapConstants.GROUP_SCOPE:
                break;
            default:
                return null;
        }
        return identity;
    }

    @Override
    public Identity untransform(Identity identity) {
        switch (identity.getKind()){
            case LdapConstants.USER_SCOPE:
                break;
            case LdapConstants.GROUP_SCOPE:
                break;
            default:
                return null;
        }
        return identity;
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        return ldapUtils.getIdentities();
    }
}
