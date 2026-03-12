package com.schemafy.domain.project.domain;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.domain.common.BaseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("workspace_members")
public class WorkspaceMember extends BaseEntity {

  private String workspaceId;
  private String userId;
  private String role;

  public static WorkspaceMember create(
      String id,
      String workspaceId,
      String userId,
      WorkspaceRole role) {
    WorkspaceMember workspaceMember = new WorkspaceMember(
        workspaceId,
        userId,
        role.name());
    workspaceMember.setId(id);
    return workspaceMember;
  }

  public WorkspaceRole getRoleAsEnum() { return WorkspaceRole.fromString(this.role); }

  public boolean isAdmin() { return getRoleAsEnum().isAdmin(); }

  public void updateRole(WorkspaceRole newRole) {
    this.role = newRole.name();
  }

}
