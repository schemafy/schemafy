package com.schemafy.core.workspace.repository.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.core.workspace.repository.vo.WorkspaceRole;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("workspace_members")
public class WorkspaceMember implements Persistable<String> {

    @Id
    @Setter(AccessLevel.PROTECTED)
    private String id;

    @Transient
    private boolean isNew = true;

    private String workspaceId;

    private String userId;

    private String role;

    private LocalDateTime joinedAt;

    private LocalDateTime deletedAt;

    public static WorkspaceMember create(String workspaceId, String userId,
            WorkspaceRole role) {
        return new WorkspaceMember(
                UlidGenerator.generate(), true, workspaceId, userId,
                role.getValue(), LocalDateTime.now(), null);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() { return deletedAt != null; }

    @Override
    public String getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    public void markAsNotNew() {
        this.isNew = false;
    }

    public WorkspaceRole getRoleAsEnum() {
        return WorkspaceRole.fromValue(this.role);
    }

    public boolean isAdmin() { return getRoleAsEnum().isAdmin(); }

    public boolean belongsToUser(String userId) {
        return this.userId.equals(userId);
    }

    public boolean belongsToWorkspace(String workspaceId) {
        return this.workspaceId.equals(workspaceId);
    }

}
