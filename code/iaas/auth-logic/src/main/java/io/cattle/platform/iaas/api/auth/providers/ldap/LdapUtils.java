package io.cattle.platform.iaas.api.auth.providers.ldap;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.iaas.api.auth.AuthUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;

public class LdapUtils extends AuthUtils {

    public boolean isAllowed(Set<ExternalId> externalIds) {
        if (externalIds.isEmpty()) {
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
        return isAllowed(externalIds(jsonData));
    }

    protected Set<ExternalId> externalIds(Map<String, Object> jsonData) {
        Set<ExternalId> externalIds = new HashSet<>();
        if (jsonData == null) {
            return externalIds;
        }
        List<String> groups = (List<String>) CollectionUtils.toList(jsonData.get(LdapConstants.LDAP_GROUPS));
        String accountId = ObjectUtils.toString(jsonData.get(LdapConstants.ACCOUNT_ID), null);
        externalIds.add(new LdapUser(accountId, (String) jsonData.get(LdapConstants.USERNAME)));
        for (String group : groups) {
            externalIds.add(new LdapGroup(group));
        }
        return externalIds;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        return true;//TODO Real white listing needed.
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
