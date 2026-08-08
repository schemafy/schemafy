package com.schemafy.api.project.controller.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.core.common.exception.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProjectSearchRequest")
class ProjectSearchRequestTest {

  @Test
  @DisplayName("워크스페이스 프로젝트 검색어를 정규화한다")
  void normalizesWorkspaceSearchText() {
    ProjectSearchRequest request = new ProjectSearchRequest(
        ProjectSearchCategory.WORKSPACE, "workspace-id", "  schema  ",
        0, 5);

    assertThat(request.workspaceId()).isEqualTo("workspace-id");
    assertThat(request.search()).isEqualTo("schema");
  }

  @Test
  @DisplayName("공유 프로젝트 검색은 workspaceId 없이 허용한다")
  void acceptsSharedProjectSearchWithoutWorkspaceId() {
    ProjectSearchRequest request = new ProjectSearchRequest(
        ProjectSearchCategory.SHARED, null, "schema", 0, 5);

    assertThat(request.workspaceId()).isNull();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { " ", "\t" })
  @DisplayName("워크스페이스 프로젝트 검색은 workspaceId를 필수로 요구한다")
  void rejectsMissingWorkspaceIdForWorkspaceSearch(String workspaceId) {
    assertInvalidParameter(
        () -> new ProjectSearchRequest(
            ProjectSearchCategory.WORKSPACE, workspaceId, "schema", 0, 5),
        "workspaceId is required");
  }

  @ParameterizedTest
  @ValueSource(strings = { "", " ", "workspace-id" })
  @DisplayName("공유 프로젝트 검색은 workspaceId를 허용하지 않는다")
  void rejectsWorkspaceIdForSharedSearch(String workspaceId) {
    assertInvalidParameter(
        () -> new ProjectSearchRequest(
            ProjectSearchCategory.SHARED, workspaceId, "schema", 0, 5),
        "workspaceId is not allowed for SHARED");
  }

  private void assertInvalidParameter(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
      String message) {
    assertThatThrownBy(action)
        .isInstanceOf(DomainException.class)
        .hasMessage(message)
        .satisfies(error -> assertThat(
            ((DomainException) error).getErrorCode())
            .isEqualTo(CommonErrorCode.INVALID_PARAMETER));
  }

}
