package io.cattle.platform.host.stats.api;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.dao.HostDao;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class HostStatsDashBoardGenerator extends AbstractResponseGenerator {

    private static final String DASHBOARD = "dashboard";

    @Inject
    HostDao hostDao;

    @Override
    protected void generate(ApiRequest request) throws IOException {
        if (!StringUtils.equals(DASHBOARD, request.getType())) {
            return;
        }
        request.setResponseObject(hostDao.getActiveHosts(((Policy) ApiContext.getContext().getPolicy()).getAccountId()));
//        request.commit();
    }
}
