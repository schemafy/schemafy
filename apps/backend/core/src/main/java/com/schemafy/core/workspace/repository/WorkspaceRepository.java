package com.schemafy.core.workspace.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.workspace.repository.entity.Workspace;

import reactor.core.publisher.Mono;

public interface WorkspaceRepository
        extends ReactiveCrudRepository<Workspace, String> {

    @Query("""
            SELECT * FROM workspaces
            WHERE id = :id AND deleted_at IS NULL
            """)
    Mono<Workspace> findByIdAndNotDeleted(String id);

    @Query("""
            SELECT * FROM workspaces
            WHERE owner_id = :ownerId AND deleted_at IS NULL
            """)
    Mono<Workspace> findByOwnerIdAndNotDeleted(String ownerId);

    @Query("""
            UPDATE workspaces
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """)
    Mono<Void> softDelete(String id);

}
