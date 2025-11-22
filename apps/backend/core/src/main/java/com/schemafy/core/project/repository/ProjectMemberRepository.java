package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.project.repository.entity.ProjectMember;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProjectMemberRepository
        extends ReactiveCrudRepository<ProjectMember, String> {

    @Query("SELECT * FROM project_members WHERE project_id = :projectId AND user_id = :userId AND deleted_at IS NULL")
    Mono<ProjectMember> findByProjectIdAndUserIdAndNotDeleted(String projectId,
            String userId);

    @Query("SELECT * FROM project_members WHERE project_id = :projectId AND deleted_at IS NULL ORDER BY joined_at LIMIT :limit OFFSET :offset")
    Flux<ProjectMember> findByProjectIdAndNotDeleted(String projectId,
            int limit, int offset);

    @Query("SELECT * FROM project_members WHERE user_id = :userId AND deleted_at IS NULL")
    Flux<ProjectMember> findByUserIdAndNotDeleted(String userId);

    @Query("SELECT COUNT(*) FROM project_members WHERE project_id = :projectId AND deleted_at IS NULL")
    Mono<Long> countByProjectIdAndNotDeleted(String projectId);

    @Query("SELECT EXISTS(SELECT 1 FROM project_members WHERE project_id = :projectId AND user_id = :userId AND deleted_at IS NULL)")
    Mono<Boolean> existsByProjectIdAndUserIdAndNotDeleted(String projectId,
            String userId);

    @Query("UPDATE project_members SET deleted_at = CURRENT_TIMESTAMP WHERE project_id = :projectId AND deleted_at IS NULL")
    Mono<Void> softDeleteByProjectId(String projectId);
}
