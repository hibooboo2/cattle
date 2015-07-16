package io.cattle.platform.iaas.api.auth.projects;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.ProjectMember;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(name = ProjectConstants.MEMBER)
public class Member {

    private final Identity identity;
    private final String role;

    public Member(ProjectMember projectMember) {
        this.role = projectMember.getRole();
        this.identity = new Identity(projectMember.getExternalIdType(), projectMember.getExternalId(), projectMember.getName());
    }

    public Member(Identity identity, String role) {
        this.role = role;
        this.identity = identity;
    }

    @Field(required = true, nullable = false)
    public String getRole() {
        return role;
    }

    @Field(required = true, nullable = false)
    public Identity getIdentity() {
        return identity;
    }

    @Override
    public String toString() {
        return "Member{" +
                "identity=" + identity.toString() +
                ", role='" + role + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Member member = (Member) o;

        if (identity != null ? !identity.equals(member.identity) : member.identity != null) return false;
        return !(role != null ? !role.equals(member.role) : member.role != null);

    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }
}
