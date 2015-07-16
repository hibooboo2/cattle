package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class LdapAuthImpl implements AccountLookup, Priority {

    @Inject
    LdapUtils ldapUtils;

    @Override
    public Account getAccount(ApiRequest request) {
        if (StringUtils.equals(LdapConstants.TOKEN, request.getType())) {
            return null;
        }
        ldapUtils.findAndSetJWT();
        return ldapUtils.getAccountFromJWT();
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }
}
