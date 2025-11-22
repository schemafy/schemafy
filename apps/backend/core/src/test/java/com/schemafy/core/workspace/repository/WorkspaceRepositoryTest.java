package com.schemafy.core.workspace.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.config.R2dbcConfig;
import com.schemafy.core.workspace.repository.entity.Workspace;
import com.schemafy.core.workspace.repository.vo.WorkspaceSettings;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(R2dbcConfig.class)
class WorkspaceRepositoryTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        workspaceRepository.deleteAll().block();

        testWorkspace = Workspace.create("owner123", "Test Workspace",
                "Test Description", WorkspaceSettings.defaultSettings());
    }

    @Test
    @DisplayName("워크스페이스 생성 테스트")
    void createWorkspace() {
        StepVerifier.create(workspaceRepository.save(testWorkspace))
                .assertNext(workspace -> {
                    assertThat(workspace.getId()).isNotNull();
                    assertThat(workspace.getName()).isEqualTo("Test Workspace");
                    assertThat(workspace.getOwnerId()).isEqualTo("owner123");
                    assertThat(workspace.getDescription()).isEqualTo(
                            "Test Description");
                }).verifyComplete();
    }

    @Test
    @DisplayName("삭제되지 않은 워크스페이스 조회")
    void findByIdAndNotDeleted() {
        Workspace saved = workspaceRepository.save(testWorkspace).block();

        StepVerifier.create(
                workspaceRepository.findByIdAndNotDeleted(saved.getId()))
                .assertNext(workspace -> {
                    assertThat(workspace.getId()).isEqualTo(saved.getId());
                    assertThat(workspace.getName()).isEqualTo("Test Workspace");
                }).verifyComplete();
    }

    @Test
    @DisplayName("삭제된 워크스페이스는 조회되지 않음")
    void findByIdAndNotDeleted_whenDeleted() {
        Workspace saved = workspaceRepository.save(testWorkspace).block();

        saved.delete();
        workspaceRepository.save(saved).block();

        StepVerifier.create(
                workspaceRepository.findByIdAndNotDeleted(saved.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Owner로 워크스페이스 조회")
    void findByOwnerIdAndNotDeleted() {
        workspaceRepository.save(testWorkspace).block();

        StepVerifier.create(
                workspaceRepository.findByOwnerIdAndNotDeleted("owner123"))
                .assertNext(workspace -> {
                    assertThat(workspace.getOwnerId()).isEqualTo("owner123");
                    assertThat(workspace.getName()).isEqualTo("Test Workspace");
                }).verifyComplete();
    }

    @Test
    @DisplayName("워크스페이스 수정 테스트")
    void updateWorkspace() {
        Workspace saved = workspaceRepository.save(testWorkspace).block();

        saved.update("Updated Name", "Updated Description",
                WorkspaceSettings.defaultSettings());
        Workspace updated = workspaceRepository.save(saved).block();

        StepVerifier.create(workspaceRepository.findById(updated.getId()))
                .assertNext(workspace -> {
                    assertThat(workspace.getName()).isEqualTo("Updated Name");
                    assertThat(workspace.getDescription()).isEqualTo(
                            "Updated Description");
                }).verifyComplete();
    }
}
