package com.schemafy.core.project.repository.entity;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.ulid.generator.UlidGenerator;

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

  public static WorkspaceMember create(String workspaceId, String userId,
      WorkspaceRole role) {
    WorkspaceMember workspaceMember = new WorkspaceMember(
        workspaceId,
        userId,
        role.getValue());
    workspaceMember.setId(UlidGenerator.generate());
    return workspaceMember;
  }

  @Override
  public boolean isDeleted() { return deletedAt != null; }

  @Override
  public String getId() { return id; }

  public WorkspaceRole getRoleAsEnum() { return WorkspaceRole.fromValue(this.role); }

  public boolean isAdmin() { return getRoleAsEnum().isAdmin(); }

  public boolean belongsToWorkspace(String workspaceId) {
    return this.workspaceId.equals(workspaceId);
  }

  public void updateRole(WorkspaceRole newRole) {
    this.role = newRole.getValue();
  }

}
