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
@Table("projects")
public class Project extends BaseEntity {

  private String workspaceId;
  private String name;
  private String description;

  public static Project create(
      String id,
      String workspaceId,
      String name,
      String description) {
    Project project = new Project(workspaceId, name, description);
    project.setId(id);
    return project;
  }

  public void update(String name, String description) {
    this.name = name;
    this.description = description;
  }

}
