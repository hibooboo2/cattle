package io.cattle.platform.api.auth;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;
import io.github.ibuildthecloud.gdapi.model.FieldType;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Type(name = "identity", pluralName = "identities")
public class Identity {

    private final String externalId;
    private final String profilePicture;
    private final String name;
    private final String externalIdType;
    private final String profileUrl;
    private final String login;
    private final String role;
    private final long projectId;

    @Field(required = false, nullable = true)
    public String getName() {
        return name;
    }

    @Field(required = true, nullable = false)
    public String getExternalId() {
        return externalId;
    }

    @Field(required = true, nullable = false)
    public String getExternalIdType() {
        return externalIdType;
    }

    @Field(required = false, nullable = true)
    public String getAll(){
        return null;
    }

    @Field(required = false, nullable = true)
    public String getId() {
        return externalIdType + ':' + externalId;
    }

    @Field(required = false, nullable = true)
    public String getProfilePicture() {
        return profilePicture;
    }

    @Field(required = false, nullable = true)
    public String getProfileUrl() {
        return profileUrl;
    }

    @Field(required = false, nullable = true)
    public String getLogin() {
        return login;
    }

    @Field(required = false, nullable = true)
    public String getRole() {
        return role;
    }

    @Field(required = false, nullable = true)
    public long getProjectId() {
        return projectId;
    }

    public Identity(String externalIdType, String externalId) {
        this(externalIdType, externalId, null, null, null, null);
    }

    public Identity(String externalIdType, String externalId, String name, String profileUrl, String profilePicture, String login) {
        this.externalId = externalId;
        this.name = name;
        this.externalIdType = externalIdType;
        this.profileUrl = profileUrl;
        this.login = login;
        if (profilePicture == null) {
            this.profilePicture = "http://robohash.org/" + getId() + ".png?set=set2";
        } else {
            this.profilePicture = profilePicture;
        }
        this.projectId = 0;
        this.role = null;
    }

    public Identity(Identity identity, String role, long projectId){
        this.externalId = identity.getExternalId();
        this.name = identity.getName();
        this.externalIdType = identity.getExternalIdType();
        this.profileUrl = identity.getProfileUrl();
        this.login = identity.getLogin();
        this.profilePicture = identity.getProfilePicture();
        this.projectId = projectId;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Identity identity = (Identity) o;

        return new EqualsBuilder()
                .append(externalId, identity.externalId)
                .append(externalIdType, identity.externalIdType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 43)
                .append(externalId)
                .append(externalIdType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("externalId", externalId)
                .append("profilePicture", profilePicture)
                .append("name", name)
                .append("externalIdType", externalIdType)
                .append("profileUrl", profileUrl)
                .toString();
    }
}
