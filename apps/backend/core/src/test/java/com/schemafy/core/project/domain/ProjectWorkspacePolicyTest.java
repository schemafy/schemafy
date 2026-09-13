package com.schemafy.core.project.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("프로젝트·워크스페이스 텍스트 정책")
class ProjectWorkspacePolicyTest {

  @Test
  @DisplayName("워크스페이스 생성과 수정은 이름과 설명 길이를 제한한다")
  void validatesWorkspaceTextOnCreateAndUpdate() {
    assertThatThrownBy(() -> Workspace.create("workspace-1", " ", null))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(WorkspaceErrorCode.SETTINGS_INVALID);
    assertThatThrownBy(() -> Workspace.create("workspace-1", "n".repeat(256), null))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(WorkspaceErrorCode.SETTINGS_TOO_LARGE);
    assertThatThrownBy(() -> Workspace.create("workspace-1", "Workspace", "d".repeat(1001)))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(WorkspaceErrorCode.SETTINGS_TOO_LARGE);

    Workspace workspace = Workspace.create("workspace-1", "Workspace", null);
    assertThatThrownBy(() -> workspace.update("n".repeat(256), null))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(WorkspaceErrorCode.SETTINGS_TOO_LARGE);
  }

  @Test
  @DisplayName("프로젝트 생성과 수정은 이름과 설명 길이를 제한한다")
  void validatesProjectTextOnCreateAndUpdate() {
    assertThatThrownBy(() -> Project.create("project-1", "workspace-1", 1, " ", null))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(ProjectErrorCode.SETTINGS_INVALID);
    assertThatThrownBy(() -> Project.create("project-1", "workspace-1", 1, "n".repeat(256), null))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(ProjectErrorCode.SETTINGS_TOO_LARGE);
    assertThatThrownBy(() -> Project.create("project-1", "workspace-1", 1, "Project", "d".repeat(1001)))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(ProjectErrorCode.SETTINGS_TOO_LARGE);

    Project project = Project.create("project-1", "workspace-1", 1, "Project", null);
    assertThatThrownBy(() -> project.update("n".repeat(256), null))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(ProjectErrorCode.SETTINGS_TOO_LARGE);
  }

  @Test
  @DisplayName("이름과 설명은 정의된 경계값까지 허용한다")
  void acceptsBoundaryValues() {
    Workspace workspace = Workspace.create("workspace-1", "n".repeat(255), "d".repeat(1000));
    Project project = Project.create("project-1", "workspace-1", 1, "n".repeat(255),
        "d".repeat(1000));

    workspace.update("n".repeat(255), "d".repeat(1000));
    project.update("n".repeat(255), "d".repeat(1000));
  }

}
