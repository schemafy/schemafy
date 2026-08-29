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

@DisplayName("MemberSearchRequest")
class MemberSearchRequestTest {

  @Test
  @DisplayName("워크스페이스 멤버 검색어를 정규화한다")
  void normalizesWorkspaceMemberSearchText() {
    MemberSearchRequest request = new MemberSearchRequest(
        MemberSearchCategory.WORKSPACE, "workspace-id", null, "  schema  ", 0,
        5);

    assertThat(request.workspaceId()).isEqualTo("workspace-id");
    assertThat(request.search()).isEqualTo("schema");
  }

  @Test
  @DisplayName("프로젝트 멤버 검색은 projectId만 허용한다")
  void acceptsProjectMemberSearchWithOnlyProjectId() {
    MemberSearchRequest request = new MemberSearchRequest(
        MemberSearchCategory.PROJECT, null, "project-id", "schema", 0, 5);

    assertThat(request.projectId()).isEqualTo("project-id");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { " ", "\t" })
  @DisplayName("워크스페이스 멤버 검색에 workspaceId가 없으면 예외가 발생한다")
  void rejectsMissingWorkspaceIdForWorkspaceMemberSearch(String workspaceId) {
    assertInvalidParameter(
        () -> new MemberSearchRequest(
            MemberSearchCategory.WORKSPACE, workspaceId, null, "schema", 0,
            5),
        "workspaceId is required");
  }

  @ParameterizedTest
  @ValueSource(strings = { "", " ", "project-id" })
  @DisplayName("워크스페이스 멤버 검색에 projectId가 있으면 예외가 발생한다")
  void rejectsProjectIdForWorkspaceMemberSearch(String projectId) {
    assertInvalidParameter(
        () -> new MemberSearchRequest(
            MemberSearchCategory.WORKSPACE, "workspace-id", projectId,
            "schema", 0, 5),
        "projectId is not allowed for WORKSPACE");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { " ", "\t" })
  @DisplayName("프로젝트 멤버 검색에 projectId가 없으면 예외가 발생한다")
  void rejectsMissingProjectIdForProjectMemberSearch(String projectId) {
    assertInvalidParameter(
        () -> new MemberSearchRequest(
            MemberSearchCategory.PROJECT, null, projectId, "schema", 0, 5),
        "projectId is required");
  }

  @ParameterizedTest
  @ValueSource(strings = { "", " ", "workspace-id" })
  @DisplayName("프로젝트 멤버 검색에 workspaceId가 있으면 예외가 발생한다")
  void rejectsWorkspaceIdForProjectMemberSearch(String workspaceId) {
    assertInvalidParameter(
        () -> new MemberSearchRequest(
            MemberSearchCategory.PROJECT, workspaceId, "project-id",
            "schema", 0, 5),
        "workspaceId is not allowed for PROJECT");
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
