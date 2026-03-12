package com.schemafy.core.project.domain;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.BaseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("workspaces")
public class Workspace extends BaseEntity {

  private String name;
  private String description;

  public static Workspace create(String id, String name, String description) {
    Workspace workspace = new Workspace(name, description);
    workspace.setId(id);
    return workspace;
  }

  public void update(String name, String description) {
    this.name = name;
    this.description = description;
  }

}
