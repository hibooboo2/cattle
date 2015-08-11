package io.cattle.platform.iaas.api.auth.interfaces;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Account;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Set;

public interface AuthorizationProvider {

    SchemaFactory getSchemaFactory(Account account, Policy policy, ApiRequest request);

    Policy getPolicy(Account account, Account authenticatedAsAccount, Set<ExternalId> externalIds, ApiRequest request);

}
