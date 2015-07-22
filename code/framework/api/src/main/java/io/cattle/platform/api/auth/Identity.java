package io.cattle.platform.api.auth;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = "identity", pluralName = "identities")
public class Identity {

    private final String externalId;
    private final String profilePicture;
    private final String name;
    private final String kind;
    private final String profileUrl;

    @Field(required = false, nullable = true)
    public String getName() {
        return name;
    }

    @Field(required = true, nullable = false)
    public String getExternalId() {
        return externalId;
    }

    @Field(required = true, nullable = false)
    public String getKind() {
        return kind;
    }

    @Field(required = false, nullable = true)
    public String getId() {
        return kind + ':' + externalId;
    }

    @Field(required = false, nullable = true)
    public String getProfilePicture() {
        return profilePicture;
    }

    @Field(required = false, nullable = true)
    public String getProfileUrl() {
        return profileUrl;
    }

    public Identity(String identityType, String externalId) {
        this(identityType, externalId, null);
    }

    public Identity(String identityType, String externalId, String name) {
        this(identityType, externalId, name, null);
    }

    public Identity(String identityType, String externalId, String name, String profileUrl) {
        this(identityType, externalId, name, profileUrl, null);
    }

    public Identity(String identityType, String externalId, String name, String profileUrl, String profilePicture) {
        this.externalId = externalId;
        this.name = name;
        this.kind = identityType;
        this.profileUrl = profileUrl;
        if (profilePicture == null) {
            this.profilePicture = "http://robohash.org/" + getId() + ".png?set=set2";
        } else {
            this.profilePicture = profilePicture;
        }
    }

    @Override
    public String toString() {
        return "Identity{" +
                "id='" + getId() +'\'' +
                "externalId=" + externalId + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", name='" + name + '\'' +
                ", kind='" + getKind() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        if (externalId != null ? !externalId.equals(identity.externalId) : identity.externalId != null) return false;
        return !(kind != null ? !kind.equals(identity.kind) : identity.kind != null);
    }

    @Override
    public int hashCode() {
        int result = externalId != null ? externalId.hashCode() : 0;
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        return result;
    }
}
