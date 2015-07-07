package io.cattle.platform.iaas.api.auth.providers.ldap;

import io.cattle.platform.api.auth.ExternalId;

public class LdapGroup extends ExternalId {

    public LdapGroup(String externalId) {
        super(externalId);
    }

    public LdapGroup(String externalId, String name) {
        super(externalId, name);
    }

    public LdapGroup(String externalId, String name, String profilePicture) {
        super(externalId, name, profilePicture);
    }

    @Override
    public String getType() {
        return LdapConstants.GROUP_SCOPE;
    }
}
