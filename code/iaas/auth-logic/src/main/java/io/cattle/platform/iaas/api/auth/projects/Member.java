package io.cattle.platform.iaas.api.auth.projects;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.ProjectMember;

public class Member  {

    private String externalId;
    private String externalIdType;
    private String role;
    private String name;

    public Member(ProjectMember projectMember) {
        this.externalId = projectMember.getExternalId();
        this.externalIdType = projectMember.getExternalIdType();
        this.role = projectMember.getRole();
        this.name = projectMember.getName();
    }

    public Member(Identity externalId, String role) {
        this.externalId = externalId.getExternalId();
        this.externalIdType = externalId.getKind();
        this.role = role;
        this.name = externalId.getName();
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalIdType() {
        return externalIdType;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Member member = (Member) o;

        if (externalId != null ? !externalId.equals(member.externalId) : member.externalId != null)
            return false;
        if (externalIdType != null ? !externalIdType.equals(member.externalIdType) : member.externalIdType != null)
            return false;
        if (role != null ? !role.equals(member.role) : member.role != null)
            return false;
        return !(name != null ? !name.equals(member.name) : member.name != null);

    }

    @Override
    public int hashCode() {
        int result = externalId != null ? externalId.hashCode() : 0;
        result = 31 * result + (externalIdType != null ? externalIdType.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }


}
