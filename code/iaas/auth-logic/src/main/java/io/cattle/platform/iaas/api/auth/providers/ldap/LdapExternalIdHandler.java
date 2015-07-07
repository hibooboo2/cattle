package io.cattle.platform.iaas.api.auth.providers.ldap;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.interfaces.ExternalIdHandler;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class LdapExternalIdHandler implements ExternalIdHandler {

    @Inject
    LdapClient ldapClient;

    @Override
    public ExternalId transform(ExternalId externalId) {
        return externalId;
    }

    @Override
    public ExternalId untransform(ExternalId externalId) {
        return externalId;
    }

    @Override
    public Set<ExternalId> getExternalIds(Account account) {
        String accessToken = (String) DataAccessor.fields(account).withKey(LdapConstants.LDAP_ACCESS_TOKEN).get();
        if (StringUtils.isBlank(accessToken)) {
            return new HashSet<>();
        }
        String[] split = accessToken.split(":");
        if (split.length != 2) {
            return new HashSet<>();
        }
        return ldapClient.getExternalIds(split[0], split[1]);

    }
}
