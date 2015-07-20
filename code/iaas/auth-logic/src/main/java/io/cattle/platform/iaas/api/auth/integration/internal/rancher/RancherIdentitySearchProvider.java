package io.cattle.platform.iaas.api.auth.integration.internal.rancher;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.identity.AbstractIdentitySearchProvider;
import io.github.ibuildthecloud.gdapi.context.ApiContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

public class RancherIdentitySearchProvider extends AbstractIdentitySearchProvider {

    @Inject
    AuthDao authDao;

    @Override
    public List<Identity> searchIdentities(String name, String scope) {
        List<Identity> identities = new ArrayList<>();
        if (!scope.equalsIgnoreCase(ProjectConstants.RANCHER_ID)){
            return identities;
        }
        List<Account> accounts = authDao.searchAccounts(name);
        for(Account account: accounts){
            identities.add(new Identity(ProjectConstants.RANCHER_ID, String.valueOf(account.getId()), account.getName()));
        }
        return identities;
    }

    @Override
    public Identity getIdentity(String id, String scope) {
        String accountId = ApiContext.getContext().getIdFormatter().parseId(id);
        Account account = authDao.getAccountById(Long.valueOf(accountId == null ? id : accountId));
        if (account == null) {
            return null;
        }
        return new Identity(ProjectConstants.RANCHER_ID, String.valueOf(account.getId()), account.getName());
    }

    @Override
    public List<String> scopesProvided() {
        return Arrays.asList(ProjectConstants.SCOPES);
    }

    @Override
    public boolean isConfigured() {
        return !SecurityConstants.SECURITY.get();
    }

    @Override
    public String getName() {
        return ProjectConstants.RANCHER_SEARCH_PROVIDER;
    }
}
