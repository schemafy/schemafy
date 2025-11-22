package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.project.repository.entity.Project;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProjectRepository
        extends ReactiveCrudRepository<Project, String> {

    @Query("SELECT * FROM projects WHERE id = :id AND deleted_at IS NULL")
    Mono<Project> findByIdAndNotDeleted(String id);

    @Query("SELECT * FROM projects WHERE workspace_id = :workspaceId AND deleted_at IS NULL")
    Flux<Project> findByWorkspaceIdAndNotDeleted(String workspaceId);

    @Query("SELECT * FROM projects WHERE owner_id = :ownerId AND deleted_at IS NULL")
    Flux<Project> findByOwnerIdAndNotDeleted(String ownerId);

    @Query("SELECT COUNT(*) FROM projects WHERE workspace_id = :workspaceId AND deleted_at IS NULL")
    Mono<Long> countByWorkspaceIdAndNotDeleted(String workspaceId);
}
