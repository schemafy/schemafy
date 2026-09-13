package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.config.R2dbcTestConfiguration;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.ulid.application.service.UlidGenerator;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ WorkspacePersistenceAdapter.class, R2dbcTestConfiguration.class })
@DisplayName("WorkspacePersistenceAdapter")
class WorkspacePersistenceAdapterTest {

  @Autowired
  private WorkspacePersistenceAdapter sut;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository workspaceMemberRepository;

  @BeforeEach
  void setUp() {
    workspaceMemberRepository.deleteAll().block();
    workspaceRepository.deleteAll().block();
  }

  @Test
  @DisplayName("save/findByIdAndNotDeleted: 워크스페이스를 저장하고 조회한다")
  void saveAndFindWorkspace() {
    Workspace workspace = Workspace.create(UlidGenerator.generate(), "Workspace",
        "Description");

    StepVerifier.create(sut.save(workspace))
        .assertNext(saved -> {
          assertThat(saved.getId()).isEqualTo(workspace.getId());
          assertThat(saved.getName()).isEqualTo("Workspace");
          assertThat(saved.getDescription()).isEqualTo("Description");
          assertThat(saved.getCreatedAt()).isNotNull();
          assertThat(saved.getUpdatedAt()).isNotNull();
        })
        .verifyComplete();

    StepVerifier.create(sut.findByIdAndNotDeleted(workspace.getId()))
        .assertNext(found -> {
          assertThat(found.getId()).isEqualTo(workspace.getId());
          assertThat(found.getName()).isEqualTo("Workspace");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("save: 로드한 워크스페이스를 수정 저장한다")
  void save_updatesLoadedWorkspace() {
    Workspace workspace = sut.save(Workspace.create(UlidGenerator.generate(),
        "Before", "Description")).block();
    Workspace loaded = workspaceRepository.findById(workspace.getId()).block();
    loaded.update("After", "Updated");

    StepVerifier.create(sut.save(loaded))
        .assertNext(updated -> {
          assertThat(updated.getId()).isEqualTo(workspace.getId());
          assertThat(updated.getName()).isEqualTo("After");
          assertThat(updated.getDescription()).isEqualTo("Updated");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("updateIfActive: 활성 워크스페이스의 이름과 설명만 수정한다")
  void updateIfActive_updatesActiveWorkspace() {
    Workspace workspace = sut.save(Workspace.create(UlidGenerator.generate(),
        "Before", "Description")).block();

    StepVerifier.create(sut.updateIfActive(workspace.getId(), "After", "Updated"))
        .expectNext(1L)
        .verifyComplete();

    StepVerifier.create(sut.findByIdAndNotDeleted(workspace.getId()))
        .assertNext(updated -> {
          assertThat(updated.getName()).isEqualTo("After");
          assertThat(updated.getDescription()).isEqualTo("Updated");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("updateIfActive: 삭제된 워크스페이스는 수정하지 않는다")
  void updateIfActive_doesNotUpdateDeletedWorkspace() {
    Workspace workspace = sut.save(Workspace.create(UlidGenerator.generate(),
        "Before", "Description")).block();
    workspace.delete();

    StepVerifier.create(sut.save(workspace))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(sut.updateIfActive(workspace.getId(), "After", "Updated"))
        .expectNext(0L)
        .verifyComplete();

    StepVerifier.create(sut.findByIdAndNotDeleted(workspace.getId()))
        .verifyComplete();

    StepVerifier.create(workspaceRepository.findById(workspace.getId()))
        .assertNext(stored -> {
          assertThat(stored.getName()).isEqualTo("Before");
          assertThat(stored.getDescription()).isEqualTo("Description");
          assertThat(stored.getDeletedAt()).isNotNull();
        })
        .verifyComplete();
  }

}
