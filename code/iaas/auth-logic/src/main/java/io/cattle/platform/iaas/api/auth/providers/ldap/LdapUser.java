package io.cattle.platform.iaas.api.auth.providers.ldap;

import io.cattle.platform.api.auth.ExternalId;

public class LdapUser extends ExternalId {

    public LdapUser(String externalId) {
        super(externalId);
    }

    public LdapUser(String externalId, String name) {
        super(externalId, name);
    }

    public LdapUser(String externalId, String name, String profilePicture) {
        super(externalId, name, profilePicture);
    }

    @Override
    public String getType() {
        return LdapConstants.USER_SCOPE;
    }
}
