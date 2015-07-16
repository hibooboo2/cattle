package io.cattle.platform.iaas.api.auth.integration.github.resource;

public class GithubAccountInfo {
    private final String accountId;
    private final String accountName;
    private final String profilePicture;
    private final String profileUrl;

    public GithubAccountInfo(String accountId, String accountName, String profilePicture, String profileUrl) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.profilePicture = profilePicture;
        this.profileUrl = profileUrl;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public String toString() {
        return accountName + ':' + accountId + ':' + profilePicture + ':' + profileUrl;

    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public String getProfileUrl() {
        return profileUrl;
    }
}
