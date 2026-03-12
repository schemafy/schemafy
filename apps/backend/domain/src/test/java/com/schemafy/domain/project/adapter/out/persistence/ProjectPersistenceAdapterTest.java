package com.schemafy.domain.project.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.config.R2dbcTestConfiguration;
import com.schemafy.domain.project.domain.Project;
import com.schemafy.domain.project.domain.Workspace;
import com.schemafy.domain.ulid.application.service.UlidGenerator;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ ProjectPersistenceAdapter.class, R2dbcTestConfiguration.class })
@DisplayName("ProjectPersistenceAdapter")
class ProjectPersistenceAdapterTest {

  @Autowired
  private ProjectPersistenceAdapter sut;

  @Autowired
  private DomainProjectRepository projectRepository;

  @Autowired
  private DomainProjectMemberRepository projectMemberRepository;

  @Autowired
  private DomainWorkspaceRepository workspaceRepository;

  @BeforeEach
  void setUp() {
    projectMemberRepository.deleteAll().block();
    projectRepository.deleteAll().block();
    workspaceRepository.deleteAll().block();
  }

  @Test
  @DisplayName("save/findByIdAndNotDeleted: 프로젝트를 저장하고 조회한다")
  void saveAndFindProject() {
    Workspace workspace = saveWorkspace("Project Workspace");
    Project project = Project.create(UlidGenerator.generate(), workspace.getId(),
        "Project", "Description");

    StepVerifier.create(sut.save(project))
        .assertNext(saved -> {
          assertThat(saved.getId()).isEqualTo(project.getId());
          assertThat(saved.getWorkspaceId()).isEqualTo(workspace.getId());
          assertThat(saved.getName()).isEqualTo("Project");
          assertThat(saved.getCreatedAt()).isNotNull();
        })
        .verifyComplete();

    StepVerifier.create(sut.findByIdAndNotDeleted(project.getId()))
        .assertNext(found -> {
          assertThat(found.getId()).isEqualTo(project.getId());
          assertThat(found.getWorkspaceId()).isEqualTo(workspace.getId());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("countByWorkspaceIdAndNotDeleted: 삭제되지 않은 프로젝트만 센다")
  void countByWorkspaceId_countsOnlyActiveProjects() {
    Workspace workspace = saveWorkspace("Count Workspace");
    Project active = sut.save(Project.create(UlidGenerator.generate(), workspace.getId(),
        "Active", "Description")).block();
    Project deleted = sut.save(Project.create(UlidGenerator.generate(), workspace.getId(),
        "Deleted", "Description")).block();
    Project loadedDeleted = projectRepository.findById(deleted.getId()).block();
    loadedDeleted.delete();
    sut.save(loadedDeleted).block();

    StepVerifier.create(sut.countByWorkspaceIdAndNotDeleted(workspace.getId()))
        .assertNext(count -> assertThat(count).isEqualTo(1L))
        .verifyComplete();

    StepVerifier.create(sut.findByWorkspaceIdAndNotDeleted(workspace.getId()).collectList())
        .assertNext(projects -> assertThat(projects)
            .extracting(Project::getId)
            .containsExactly(active.getId()))
        .verifyComplete();
  }

  @Test
  @DisplayName("save: 로드한 프로젝트를 수정 저장한다")
  void save_updatesLoadedProject() {
    Workspace workspace = saveWorkspace("Update Workspace");
    Project project = sut.save(Project.create(UlidGenerator.generate(),
        workspace.getId(), "Before", "Description")).block();
    Project loaded = projectRepository.findById(project.getId()).block();
    loaded.update("After", "Updated");

    StepVerifier.create(sut.save(loaded))
        .assertNext(updated -> {
          assertThat(updated.getId()).isEqualTo(project.getId());
          assertThat(updated.getName()).isEqualTo("After");
          assertThat(updated.getDescription()).isEqualTo("Updated");
        })
        .verifyComplete();
  }

  private Workspace saveWorkspace(String name) {
    return workspaceRepository.save(Workspace.create(UlidGenerator.generate(), name,
        "Description")).block();
  }

}
