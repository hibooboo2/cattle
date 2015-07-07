package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;

public class LdapUtils extends TokenUtils {

    public boolean isAllowed(Set<Identity> identities) {
        if (identities.isEmpty()) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        return true;
    }

    @Override
    protected String getAccountType() {
        return LdapConstants.USER_SCOPE;
    }

    @Override
    protected String tokenType() {
        return LdapConstants.LDAP_JWT;
    }

    @Override
    protected boolean isAllowed(Map<String, Object> jsonData) {
        return isAllowed(identities(jsonData));
    }

    @SuppressWarnings("unchecked")
    protected Set<Identity> identities(Map<String, Object> jsonData) {
        Set<Identity> identities = new HashSet<>();
        if (jsonData == null) {
            return identities;
        }
        List<String> groups = (List<String>) CollectionUtils.toList(jsonData.get(LdapConstants.LDAP_GROUPS));
        String accountId = ObjectUtils.toString(jsonData.get(TokenUtils.ACCOUNT_ID), null);
        identities.add(new Identity(LdapConstants.USER_SCOPE, accountId));
        for (String group : groups) {
            identities.add(new Identity(LdapConstants.GROUP_SCOPE, group));
        }
        return identities;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        //TODO Real white listing needed.
        return true;
    }

    @Override
    protected String accessMode() {
        return LdapConstants.ACCESS_MODE.get();
    }

    @Override
    protected String accessToken() {
        return LdapConstants.LDAP_ACCESS_TOKEN;
    }
}
