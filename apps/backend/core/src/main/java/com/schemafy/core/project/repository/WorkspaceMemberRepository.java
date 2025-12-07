package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.repository.entity.WorkspaceMember;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspaceMemberRepository
        extends ReactiveCrudRepository<WorkspaceMember, String> {

    @Query("""
            SELECT * FROM workspace_members
            WHERE workspace_id = :workspaceId
              AND user_id = :userId
              AND deleted_at IS NULL
            """)
    Mono<WorkspaceMember> findByWorkspaceIdAndUserIdAndNotDeleted(
            String workspaceId, String userId);

    @Query("""
            SELECT * FROM workspace_members
            WHERE user_id = :userId
              AND deleted_at IS NULL
            ORDER BY created_at DESC
            """)
    Flux<WorkspaceMember> findByUserIdAndNotDeleted(String userId);

    @Query("""
            SELECT * FROM workspace_members
            WHERE workspace_id = :workspaceId
              AND deleted_at IS NULL
            ORDER BY created_at ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<WorkspaceMember> findByWorkspaceIdAndNotDeleted(String workspaceId,
            int limit, int offset);

    @Query("""
            SELECT COUNT(*) FROM workspace_members
            WHERE workspace_id = :workspaceId
              AND deleted_at IS NULL
            """)
    Mono<Long> countByWorkspaceIdAndNotDeleted(String workspaceId);

    @Query("""
            UPDATE workspace_members
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE workspace_id = :workspaceId
            """)
    Mono<Void> softDeleteByWorkspaceId(String workspaceId);

    @Query("""
            SELECT EXISTS(
                SELECT 1 FROM workspace_members
                WHERE workspace_id = :workspaceId
                  AND user_id = :userId
                  AND deleted_at IS NULL
            )
            """)
    Mono<Boolean> existsByWorkspaceIdAndUserIdAndNotDeleted(String workspaceId,
            String userId);

    @Query("""
            SELECT COUNT(*) FROM workspace_members
            WHERE workspace_id = :workspaceId
              AND role = :role
              AND deleted_at IS NULL
            """)
    Mono<Long> countByWorkspaceIdAndRoleAndNotDeleted(String workspaceId,
            String role);

    @Query("""
            SELECT * FROM workspace_members
            WHERE id = :id
              AND deleted_at IS NULL
            """)
    Mono<WorkspaceMember> findByIdAndNotDeleted(String id);

}
