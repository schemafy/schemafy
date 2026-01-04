package com.schemafy.core.project.repository.entity;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.project.repository.vo.ProjectSettings;
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

    private String settings;

    public static Project create(String workspaceId,
            String name, String description, ProjectSettings settings) {
        Project project = new Project(workspaceId, name, description,
                settings.toJson());
        project.setId(UlidGenerator.generate());
        return project;
    }

    public void update(String name, String description,
            ProjectSettings settings) {
        this.name = name;
        this.description = description;
        this.settings = settings.toJson();
    }

    public ProjectSettings getSettingsAsVo() {
        return ProjectSettings.fromJson(this.settings);
    }

    public boolean belongsToWorkspace(String workspaceId) {
        return this.workspaceId.equals(workspaceId);
    }

}
