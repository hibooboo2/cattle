package io.cattle.platform.iaas.api.auth.projects;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.ProjectMember;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

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

        return new EqualsBuilder()
                .append(externalId, member.externalId)
                .append(externalIdType, member.externalIdType)
                .append(role, member.role)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(externalId)
                .append(externalIdType)
                .append(role)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("externalId", externalId)
                .append("externalIdType", externalIdType)
                .append("role", role)
                .append("name", name)
                .toString();
    }
}
