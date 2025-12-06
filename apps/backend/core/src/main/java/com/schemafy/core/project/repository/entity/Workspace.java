package com.schemafy.core.project.repository.entity;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("workspaces")
public class Workspace extends BaseEntity {

    private String ownerId;

    private String name;

    private String description;

    private String settings;

    public static Workspace create(String ownerId, String name,
            String description, WorkspaceSettings settings) {
        Workspace workspace = new Workspace(ownerId, name, description,
                settings.toJson());
        workspace.setId(UlidGenerator.generate());
        return workspace;
    }

    public void update(String name, String description,
            WorkspaceSettings settings) {
        this.name = name;
        this.description = description;
        this.settings = settings.toJson();
    }

    public WorkspaceSettings getSettingsAsVo() {
        return WorkspaceSettings.fromJson(this.settings);
    }

}
