package com.schemafy.domain.project.domain;

import java.time.Instant;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.domain.common.BaseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("project_members")
public class ProjectMember extends BaseEntity {

  private String projectId;
  private String userId;
  private String role;
  private Instant joinedAt;

  public static ProjectMember create(
      String id,
      String projectId,
      String userId,
      ProjectRole role) {
    ProjectMember member = new ProjectMember(projectId, userId,
        role.name(), Instant.now());
    member.setId(id);
    return member;
  }

  public void updateRole(ProjectRole role) {
    this.role = role.name();
  }

  public ProjectRole getRoleAsEnum() { return ProjectRole.fromString(this.role); }

  public boolean isAdmin() { return getRoleAsEnum().isAdmin(); }

  public boolean canEdit() { return getRoleAsEnum().canEdit(); }

}
