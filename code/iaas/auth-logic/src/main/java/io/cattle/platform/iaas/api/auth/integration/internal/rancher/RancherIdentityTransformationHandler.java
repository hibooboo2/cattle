package io.cattle.platform.iaas.api.auth.integration.internal.rancher;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityTransformationHandler;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

public class RancherIdentityTransformationHandler implements IdentityTransformationHandler {

    @Inject
    AuthDao authDao;
    @Inject
    ObjectManager objectManager;

    @Override
    public Identity transform(Identity identity) {
        if (identity.getKind().equalsIgnoreCase(ProjectConstants.RANCHER_ID)) {
            String accountId = ApiContext.getContext().getIdFormatter().parseId(identity.getId());
            Account account = authDao.getAccountById(Long.valueOf(accountId));
            if (account != null) {
                return new Identity(ProjectConstants.RANCHER_ID, String.valueOf(account.getId()), account.getName());
            }
        }
        return null;
    }

    @Override
    public Identity untransform(Identity identity) {
        if (identity.getKind().equalsIgnoreCase(ProjectConstants.RANCHER_ID)) {
            Account account = authDao.getAccountById(Long.valueOf(identity.getId()));
            if (account != null) {
                String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
                return new Identity(ProjectConstants.RANCHER_ID, accountId, account.getName());
            }
        }
        return null;
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        Set<Identity> identities = new HashSet<>();
        identities.add(new Identity(ProjectConstants.RANCHER_ID, String.valueOf(account.getId()), account.getName()));
        return identities;
    }
}
