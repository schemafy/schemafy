package com.schemafy.core.project.repository.entity;

import java.time.Instant;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.ulid.generator.UlidGenerator;

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

  public static ProjectMember create(String projectId, String userId,
      ProjectRole role) {
    ProjectMember member = new ProjectMember(projectId, userId,
        role.getValue(), Instant.now());
    member.setId(UlidGenerator.generate());
    return member;
  }

  public void updateRole(ProjectRole role) {
    this.role = role.getValue();
  }

  public ProjectRole getRoleAsEnum() { return ProjectRole.fromString(this.role); }

  public boolean isAdmin() { return getRoleAsEnum().isAdmin(); }

  public boolean canEdit() {
    return getRoleAsEnum().canEdit();
  }

}
