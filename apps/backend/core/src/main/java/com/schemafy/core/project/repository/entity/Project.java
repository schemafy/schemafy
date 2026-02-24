package com.schemafy.core.project.repository.entity;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("projects")
public class Project extends BaseEntity {

  private String workspaceId;
  private String name;
  private String description;

  public static Project create(String workspaceId,
      String name, String description) {
    Project project = new Project(workspaceId, name, description);
    project.setId(UlidGenerator.generate());
    return project;
  }

  public void update(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public void belongsToWorkspace(String workspaceId) {
    if (!this.workspaceId.equals(workspaceId)) {
      throw new BusinessException(ErrorCode.PROJECT_WORKSPACE_MISMATCH);
    }
  }

}
