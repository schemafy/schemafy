package com.schemafy.core.project.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

/** Project API 문서화를 위한 스니펫 제공 클래스 */
public class ProjectApiSnippets extends RestDocsSnippets {

  // ========== Project 도메인 공통 필드 ==========

  /** ProjectSettings 응답 필드 */
  private static FieldDescriptor[] projectSettingsFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "theme").type(JsonFieldType.STRING)
          .description("테마 설정 (예: light, dark)"),
      fieldWithPath(prefix + "language").type(JsonFieldType.STRING)
          .description("언어 설정 (예: ko, en)")
    };
  }

  /** Project 응답 필드 (상세 정보) */
  private static FieldDescriptor[] projectResponseFields() {
    return concat(
        new FieldDescriptor[] {
          fieldWithPath("result.id").type(JsonFieldType.STRING)
              .description("프로젝트 고유 ID"),
          fieldWithPath("result.workspaceId")
              .type(JsonFieldType.STRING)
              .description("소속 워크스페이스 ID"),
          fieldWithPath("result.name").type(JsonFieldType.STRING)
              .description("프로젝트 이름"),
          fieldWithPath("result.description")
              .type(JsonFieldType.STRING)
              .description("프로젝트 설명").optional(),
          fieldWithPath("result.settings").type(JsonFieldType.OBJECT)
              .description("프로젝트 설정"),
          fieldWithPath("result.createdAt").type(JsonFieldType.STRING)
              .description("생성 시각"),
          fieldWithPath("result.updatedAt").type(JsonFieldType.STRING)
              .description("수정 시각"),
          fieldWithPath("result.currentUserRole").type(JsonFieldType.STRING)
              .description("현재 사용자의 역할 (OWNER, ADMIN, EDITOR, COMMENTER, VIEWER)")
        },
        projectSettingsFields("result.settings."));
  }

  /** ProjectSummary 응답 필드 (목록 조회용) */
  private static FieldDescriptor[] projectSummaryFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("프로젝트 고유 ID"),
      fieldWithPath(prefix + "workspaceId").type(JsonFieldType.STRING)
          .description("소속 워크스페이스 ID"),
      fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
          .description("프로젝트 이름"),
      fieldWithPath(prefix + "description").type(JsonFieldType.STRING)
          .description("프로젝트 설명").optional(),
      fieldWithPath(prefix + "myRole").type(JsonFieldType.STRING)
          .description(
              "현재 사용자의 역할 (OWNER, ADMIN, EDITOR, COMMENTER, VIEWER)"),
      fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING)
          .description("생성 시각"),
      fieldWithPath(prefix + "updatedAt").type(JsonFieldType.STRING)
          .description("수정 시각")
    };
  }

  // ========== POST /api/workspaces/{workspaceId}/projects - 프로젝트 생성 ==========

  /** 프로젝트 생성 경로 파라미터 */
  public static Snippet createProjectPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"));
  }

  /** 프로젝트 생성 요청 헤더 */
  public static Snippet createProjectRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 프로젝트 생성 요청 바디 */
  public static Snippet createProjectRequest() {
    return requestFields(
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("프로젝트 이름 (필수)"),
        fieldWithPath("description").type(JsonFieldType.STRING)
            .description("프로젝트 설명").optional(),
        fieldWithPath("settings").type(JsonFieldType.OBJECT)
            .description("프로젝트 설정 (null인 경우 기본값 사용)").optional(),
        fieldWithPath("settings.theme").type(JsonFieldType.STRING)
            .description("테마 설정").optional(),
        fieldWithPath("settings.language").type(JsonFieldType.STRING)
            .description("언어 설정").optional(),
        fieldWithPath("settings.defaultView").type(JsonFieldType.STRING)
            .description("기본 뷰 설정").optional());
  }

  /** 프로젝트 생성 응답 헤더 */
  public static Snippet createProjectResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 프로젝트 생성 응답 */
  public static Snippet createProjectResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(projectResponseFields()));
  }

  // ========== GET /api/workspaces/{workspaceId}/projects - 프로젝트 목록 조회 ==========

  /** 프로젝트 목록 조회 경로 파라미터 */
  public static Snippet getProjectsPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"));
  }

  /** 프로젝트 목록 조회 요청 헤더 */
  public static Snippet getProjectsRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 프로젝트 목록 조회 쿼리 파라미터 */
  public static Snippet getProjectsQueryParameters() {
    return queryParameters(
        parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)")
            .optional(),
        parameterWithName("size")
            .description("페이지 크기 (기본값: 20, 최대: 100)").optional());
  }

  /** 프로젝트 목록 조회 응답 헤더 */
  public static Snippet getProjectsResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 프로젝트 목록 조회 응답 */
  public static Snippet getProjectsResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(concat(
            new FieldDescriptor[] {
              fieldWithPath("result.content[]")
                  .type(JsonFieldType.ARRAY)
                  .description("프로젝트 목록"),
              fieldWithPath("result.page")
                  .type(JsonFieldType.NUMBER)
                  .description("현재 페이지 번호 (0부터 시작)"),
              fieldWithPath("result.size")
                  .type(JsonFieldType.NUMBER)
                  .description("페이지 크기"),
              fieldWithPath("result.totalElements")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 프로젝트 개수"),
              fieldWithPath("result.totalPages")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 페이지 수")
            },
            projectSummaryFields("result.content[]."))));
  }

  // ========== GET /api/workspaces/{workspaceId}/projects/{id} - 프로젝트 상세 조회 ==========

  /** 프로젝트 상세 조회 경로 파라미터 */
  public static Snippet getProjectPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"),
        parameterWithName("id").description("프로젝트 ID"));
  }

  /** 프로젝트 상세 조회 요청 헤더 */
  public static Snippet getProjectRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 프로젝트 상세 조회 응답 헤더 */
  public static Snippet getProjectResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 프로젝트 상세 조회 응답 */
  public static Snippet getProjectResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(projectResponseFields()));
  }

  // ========== PUT /api/workspaces/{workspaceId}/projects/{id} - 프로젝트 수정 ==========

  /** 프로젝트 수정 경로 파라미터 */
  public static Snippet updateProjectPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"),
        parameterWithName("id").description("프로젝트 ID"));
  }

  /** 프로젝트 수정 요청 헤더 */
  public static Snippet updateProjectRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 프로젝트 수정 요청 바디 */
  public static Snippet updateProjectRequest() {
    return requestFields(
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("프로젝트 이름 (필수)"),
        fieldWithPath("description").type(JsonFieldType.STRING)
            .description("프로젝트 설명").optional(),
        fieldWithPath("settings").type(JsonFieldType.OBJECT)
            .description("프로젝트 설정 (null인 경우 기본값 사용)").optional(),
        fieldWithPath("settings.theme").type(JsonFieldType.STRING)
            .description("테마 설정").optional(),
        fieldWithPath("settings.language").type(JsonFieldType.STRING)
            .description("언어 설정").optional());
  }

  /** 프로젝트 수정 응답 헤더 */
  public static Snippet updateProjectResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 프로젝트 수정 응답 */
  public static Snippet updateProjectResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(projectResponseFields()));
  }

  // ========== DELETE /api/workspaces/{workspaceId}/projects/{id} - 프로젝트 삭제 ==========

  /** 프로젝트 삭제 경로 파라미터 */
  public static Snippet deleteProjectPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"),
        parameterWithName("id").description("프로젝트 ID"));
  }

  /** 프로젝트 삭제 요청 헤더 */
  public static Snippet deleteProjectRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  // ========== GET /api/workspaces/{workspaceId}/projects/{id}/members - 프로젝트 멤버 조회 ==========

  /** 프로젝트 멤버 조회 경로 파라미터 */
  public static Snippet getProjectMembersPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"),
        parameterWithName("id").description("프로젝트 ID"));
  }

  /** 프로젝트 멤버 조회 요청 헤더 */
  public static Snippet getProjectMembersRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 프로젝트 멤버 조회 쿼리 파라미터 */
  public static Snippet getProjectMembersQueryParameters() {
    return queryParameters(
        parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)")
            .optional(),
        parameterWithName("size")
            .description("페이지 크기 (기본값: 20, 최대: 100)").optional());
  }

  /** 프로젝트 멤버 조회 응답 헤더 */
  public static Snippet getProjectMembersResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 프로젝트 멤버 조회 응답 */
  public static Snippet getProjectMembersResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            fieldWithPath("result.content[]")
                .type(JsonFieldType.ARRAY)
                .description("멤버 목록"),
            fieldWithPath("result.content[].projectId")
                .type(JsonFieldType.STRING)
                .description("프로젝트 ID"),
            fieldWithPath("result.content[].userId")
                .type(JsonFieldType.STRING)
                .description("사용자 ID"),
            fieldWithPath("result.content[].userName")
                .type(JsonFieldType.STRING)
                .description("사용자 이름"),
            fieldWithPath("result.content[].userEmail")
                .type(JsonFieldType.STRING)
                .description("사용자 이메일"),
            fieldWithPath("result.content[].role")
                .type(JsonFieldType.STRING)
                .description(
                    "프로젝트 내 역할 (OWNER, ADMIN, EDITOR, COMMENTER, VIEWER)"),
            fieldWithPath("result.content[].joinedAt")
                .type(JsonFieldType.STRING)
                .description("가입 시각"),
            fieldWithPath("result.page").type(JsonFieldType.NUMBER)
                .description("현재 페이지 번호 (0부터 시작)"),
            fieldWithPath("result.size").type(JsonFieldType.NUMBER)
                .description("페이지 크기"),
            fieldWithPath("result.totalElements")
                .type(JsonFieldType.NUMBER)
                .description("전체 멤버 수"),
            fieldWithPath("result.totalPages")
                .type(JsonFieldType.NUMBER)
                .description("전체 페이지 수")));
  }

  // ========== PATCH /api/workspaces/{workspaceId}/projects/{projectId}/members/{userId}/role - 멤버 역할 변경 ==========

  /** 멤버 역할 변경 경로 파라미터 */
  public static Snippet updateMemberRolePathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"),
        parameterWithName("projectId").description("프로젝트 ID"),
        parameterWithName("userId").description("사용자 ID"));
  }

  /** 멤버 역할 변경 요청 헤더 */
  public static Snippet updateMemberRoleRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 멤버 역할 변경 요청 바디 */
  public static Snippet updateMemberRoleRequest() {
    return requestFields(
        fieldWithPath("role").type(JsonFieldType.STRING)
            .description(
                "변경할 역할 (OWNER, ADMIN, EDITOR, COMMENTER, VIEWER)"));
  }

  /** 멤버 역할 변경 응답 헤더 */
  public static Snippet updateMemberRoleResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 멤버 역할 변경 응답 */
  public static Snippet updateMemberRoleResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            fieldWithPath("result.projectId").type(JsonFieldType.STRING)
                .description("프로젝트 ID"),
            fieldWithPath("result.userId")
                .type(JsonFieldType.STRING)
                .description("사용자 ID"),
            fieldWithPath("result.userName")
                .type(JsonFieldType.STRING)
                .description("사용자 이름"),
            fieldWithPath("result.userEmail")
                .type(JsonFieldType.STRING)
                .description("사용자 이메일"),
            fieldWithPath("result.role").type(JsonFieldType.STRING)
                .description("변경된 역할"),
            fieldWithPath("result.joinedAt")
                .type(JsonFieldType.STRING)
                .description("가입 시각")));
  }

  // ========== DELETE /api/workspaces/{workspaceId}/projects/{projectId}/members/{userId} - 멤버 제거 ==========

  /** 멤버 제거 경로 파라미터 */
  public static Snippet removeMemberPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"),
        parameterWithName("projectId").description("프로젝트 ID"),
        parameterWithName("userId").description("사용자 ID"));
  }

  /** 멤버 제거 요청 헤더 */
  public static Snippet removeMemberRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  // ========== DELETE /api/workspaces/{workspaceId}/projects/{projectId}/members/me - 프로젝트 탈퇴 ==========

  /** 프로젝트 탈퇴 경로 파라미터 */
  public static Snippet leaveProjectPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"),
        parameterWithName("projectId").description("프로젝트 ID"));
  }

  /** 프로젝트 탈퇴 요청 헤더 */
  public static Snippet leaveProjectRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

}
