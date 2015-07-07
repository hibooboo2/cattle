package io.cattle.platform.api.auth;

public class ExternalId {

    private final String id;
    private final String profilePicture;
    private final String name;
    private final String type;

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public ExternalId(String externalId) {
        this(externalId, null);
    }

    public ExternalId(String externalId, String name) {
        this(externalId, name, null);
    }

    public ExternalId(String externalId, String name, String profilePicture) {
        this(externalId, name, profilePicture, null);
    }

    public ExternalId(String externalId, String name, String profilePicture, String externalIdType) {
        this.id = externalId;
        this.profilePicture = profilePicture;
        this.name = name;
        this.type = externalIdType;
    }

    @Override
    public String toString() {
        return "ExternalId{" +
                "id='" + id + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", name='" + name + '\'' +
                ", type='" + getType() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExternalId that = (ExternalId) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (getType() != null ? !getType().equals(that.getType()) : that.getType() != null) return false;
        if (profilePicture != null ? !profilePicture.equals(that.profilePicture) : that.profilePicture != null) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (profilePicture != null ? profilePicture.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        return result;
    }
}
