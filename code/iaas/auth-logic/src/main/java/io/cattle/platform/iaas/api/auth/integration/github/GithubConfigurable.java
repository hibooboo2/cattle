package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.interfaces.Configurable;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public abstract class GithubConfigurable implements Configurable{

    @Inject
    GithubClient githubClient;

    @Override
    public boolean isConfigured() {
        if (SecurityConstants.SECURITY.get()) {
            return StringUtils.equalsIgnoreCase(SecurityConstants.AUTHPROVIDER.get(), GithubConstants.CONFIG) &&
                    StringUtils.isNotBlank(GithubConstants.GITHUB_CLIENT_ID.get()) &&
                    StringUtils.isNotBlank(GithubConstants.GITHUB_CLIENT_SECRET.get());
        } else {
            return StringUtils.isNotBlank(GithubConstants.GITHUB_CLIENT_ID.get()) &&
                    StringUtils.isNotBlank(GithubConstants.GITHUB_CLIENT_SECRET.get());
        }
    }
}
