package com.schemafy.core.project.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.config.R2dbcConfig;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.vo.ProjectSettings;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(R2dbcConfig.class)
@DisplayName("ProjectRepository 테스트")
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    private Project testProject;
    private String testWorkspaceId = "workspace123";
    private String testOwnerId = "owner123";

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll().block();

        testProject = Project.create(testWorkspaceId, testOwnerId,
                "Test Project", "Test Description",
                ProjectSettings.defaultSettings());
    }

    @Test
    @DisplayName("프로젝트 생성 테스트")
    void createProject() {
        StepVerifier.create(projectRepository.save(testProject))
                .assertNext(project -> {
                    assertThat(project.getId()).isNotNull();
                    assertThat(project.getName()).isEqualTo("Test Project");
                    assertThat(project.getWorkspaceId())
                            .isEqualTo(testWorkspaceId);
                    assertThat(project.getOwnerId()).isEqualTo(testOwnerId);
                    assertThat(project.getDescription())
                            .isEqualTo("Test Description");
                }).verifyComplete();
    }

    @Test
    @DisplayName("삭제되지 않은 프로젝트 조회")
    void findByIdAndNotDeleted() {
        Project saved = projectRepository.save(testProject).block();

        StepVerifier
                .create(projectRepository.findByIdAndNotDeleted(saved.getId()))
                .assertNext(project -> {
                    assertThat(project.getId()).isEqualTo(saved.getId());
                    assertThat(project.getName()).isEqualTo("Test Project");
                }).verifyComplete();
    }

    @Test
    @DisplayName("삭제된 프로젝트는 조회되지 않음")
    void findByIdAndNotDeleted_whenDeleted() {
        Project saved = projectRepository.save(testProject).block();

        saved.delete();
        projectRepository.save(saved).block();

        StepVerifier
                .create(projectRepository.findByIdAndNotDeleted(saved.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("워크스페이스로 프로젝트 조회")
    void findByWorkspaceIdAndNotDeleted() {
        projectRepository.save(testProject).block();

        StepVerifier
                .create(projectRepository
                        .findByWorkspaceIdAndNotDeleted(testWorkspaceId))
                .assertNext(project -> {
                    assertThat(project.getWorkspaceId())
                            .isEqualTo(testWorkspaceId);
                    assertThat(project.getName()).isEqualTo("Test Project");
                }).verifyComplete();
    }

    @Test
    @DisplayName("Owner로 프로젝트 조회")
    void findByOwnerIdAndNotDeleted() {
        projectRepository.save(testProject).block();

        StepVerifier
                .create(projectRepository
                        .findByOwnerIdAndNotDeleted(testOwnerId))
                .assertNext(project -> {
                    assertThat(project.getOwnerId()).isEqualTo(testOwnerId);
                    assertThat(project.getName()).isEqualTo("Test Project");
                }).verifyComplete();
    }

    @Test
    @DisplayName("프로젝트 수정 테스트")
    void updateProject() {
        Project saved = projectRepository.save(testProject).block();

        saved.update("Updated Name", "Updated Description",
                ProjectSettings.defaultSettings());
        Project updated = projectRepository.save(saved).block();

        StepVerifier.create(projectRepository.findById(updated.getId()))
                .assertNext(project -> {
                    assertThat(project.getName()).isEqualTo("Updated Name");
                    assertThat(project.getDescription())
                            .isEqualTo("Updated Description");
                }).verifyComplete();
    }

    @Test
    @DisplayName("워크스페이스 내 프로젝트 개수 조회")
    void countByWorkspaceIdAndNotDeleted() {
        projectRepository.save(testProject).block();

        Project anotherProject = Project.create(testWorkspaceId, testOwnerId,
                "Another Project", "Description",
                ProjectSettings.defaultSettings());
        projectRepository.save(anotherProject).block();

        StepVerifier
                .create(projectRepository
                        .countByWorkspaceIdAndNotDeleted(testWorkspaceId))
                .assertNext(count -> {
                    assertThat(count).isEqualTo(2L);
                }).verifyComplete();
    }

    @Test
    @DisplayName("삭제된 프로젝트는 개수에 포함되지 않음")
    void countByWorkspaceIdAndNotDeleted_excludesDeleted() {
        Project saved = projectRepository.save(testProject).block();

        saved.delete();
        projectRepository.save(saved).block();

        StepVerifier
                .create(projectRepository
                        .countByWorkspaceIdAndNotDeleted(testWorkspaceId))
                .assertNext(count -> {
                    assertThat(count).isEqualTo(0L);
                }).verifyComplete();
    }
}
