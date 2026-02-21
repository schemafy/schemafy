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

/** Workspace API 문서화를 위한 스니펫 제공 클래스 */
public class WorkspaceApiSnippets extends RestDocsSnippets {

  // ========== Workspace 도메인 공통 필드 ==========

  /** WorkspaceSettings 응답 필드 */
  private static FieldDescriptor[] workspaceSettingsFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "language").type(JsonFieldType.STRING)
          .description("언어 설정 (ko, en)")
    };
  }

  /** Workspace 응답 필드 (상세 정보) */
  private static FieldDescriptor[] workspaceResponseFields() {
    return concat(
        new FieldDescriptor[] {
          fieldWithPath("id").type(JsonFieldType.STRING)
              .description("워크스페이스 고유 ID (ULID)"),
          fieldWithPath("name").type(JsonFieldType.STRING)
              .description("워크스페이스 이름 (1-255자)"),
          fieldWithPath("description")
              .type(JsonFieldType.STRING)
              .description("워크스페이스 설명 (최대 1000자)").optional(),
          fieldWithPath("ownerId").type(JsonFieldType.STRING)
              .description("워크스페이스 소유자 ID (ULID)"),
          fieldWithPath("settings").type(JsonFieldType.OBJECT)
              .description("워크스페이스 설정"),
          fieldWithPath("createdAt").type(JsonFieldType.STRING)
              .description("생성 시각 (ISO 8601)"),
          fieldWithPath("updatedAt").type(JsonFieldType.STRING)
              .description("수정 시각 (ISO 8601)")
        },
        workspaceSettingsFields("settings."));
  }

  /** WorkspaceSummary 응답 필드 (목록 조회용) */
  private static FieldDescriptor[] workspaceSummaryFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("워크스페이스 고유 ID (ULID)"),
      fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
          .description("워크스페이스 이름"),
      fieldWithPath(prefix + "description").type(JsonFieldType.STRING)
          .description("워크스페이스 설명").optional(),
      fieldWithPath(prefix + "ownerId").type(JsonFieldType.STRING)
          .description("워크스페이스 소유자 ID"),
      fieldWithPath(prefix + "memberCount").type(JsonFieldType.NUMBER)
          .description("전체 멤버 수"),
      fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING)
          .description("생성 시각 (ISO 8601)"),
      fieldWithPath(prefix + "updatedAt").type(JsonFieldType.STRING)
          .description("수정 시각 (ISO 8601)")
    };
  }

  // ========== POST /api/workspaces - 워크스페이스 생성 ==========

  /** 워크스페이스 생성 요청 바디 */
  public static Snippet createWorkspaceRequest() {
    return requestFields(
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("워크스페이스 이름 (1-255자, 필수)"),
        fieldWithPath("description").type(JsonFieldType.STRING)
            .description("워크스페이스 설명 (최대 1000자)").optional(),
        fieldWithPath("settings").type(JsonFieldType.OBJECT)
            .description("워크스페이스 설정 (null인 경우 기본값 사용)").optional(),
        fieldWithPath("settings.theme").type(JsonFieldType.STRING)
            .description("테마 설정 (light, dark)").optional(),
        fieldWithPath("settings.language").type(JsonFieldType.STRING)
            .description("언어 설정 (ko, en)").optional());
  }

  /** 워크스페이스 생성 요청 헤더 */
  public static Snippet createWorkspaceRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 워크스페이스 생성 응답 헤더 */
  public static Snippet createWorkspaceResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 워크스페이스 생성 응답 */
  public static Snippet createWorkspaceResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(workspaceResponseFields()));
  }

  // ========== GET /api/workspaces - 워크스페이스 목록 조회 ==========

  /** 워크스페이스 목록 조회 요청 헤더 */
  public static Snippet getWorkspacesRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 워크스페이스 목록 조회 쿼리 파라미터 */
  public static Snippet getWorkspacesQueryParameters() {
    return queryParameters(
        parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)")
            .optional(),
        parameterWithName("size")
            .description("페이지 크기 (기본값: 20, 최대: 100)").optional());
  }

  /** 워크스페이스 목록 조회 응답 헤더 */
  public static Snippet getWorkspacesResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 워크스페이스 목록 조회 응답 */
  public static Snippet getWorkspacesResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(concat(
            new FieldDescriptor[] {
              fieldWithPath("content[]")
                  .type(JsonFieldType.ARRAY)
                  .description("워크스페이스 목록"),
              fieldWithPath("page")
                  .type(JsonFieldType.NUMBER)
                  .description("현재 페이지 번호 (0부터 시작)"),
              fieldWithPath("size")
                  .type(JsonFieldType.NUMBER)
                  .description("페이지 크기"),
              fieldWithPath("totalElements")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 워크스페이스 개수"),
              fieldWithPath("totalPages")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 페이지 수")
            },
            workspaceSummaryFields("content[]."))));
  }

  // ========== GET /api/workspaces/{id} - 워크스페이스 상세 조회 ==========

  /** 워크스페이스 상세 조회 경로 파라미터 */
  public static Snippet getWorkspacePathParameters() {
    return pathParameters(
        parameterWithName("id").description("워크스페이스 ID (ULID)"));
  }

  /** 워크스페이스 상세 조회 요청 헤더 */
  public static Snippet getWorkspaceRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 워크스페이스 상세 조회 응답 헤더 */
  public static Snippet getWorkspaceResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 워크스페이스 상세 조회 응답 */
  public static Snippet getWorkspaceResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(workspaceResponseFields()));
  }

  // ========== PUT /api/workspaces/{id} - 워크스페이스 수정 ==========

  /** 워크스페이스 수정 경로 파라미터 */
  public static Snippet updateWorkspacePathParameters() {
    return pathParameters(
        parameterWithName("id").description("워크스페이스 ID (ULID)"));
  }

  /** 워크스페이스 수정 요청 헤더 */
  public static Snippet updateWorkspaceRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 워크스페이스 수정 요청 바디 */
  public static Snippet updateWorkspaceRequest() {
    return requestFields(
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("워크스페이스 이름 (1-255자, 필수)"),
        fieldWithPath("description").type(JsonFieldType.STRING)
            .description("워크스페이스 설명 (최대 1000자)").optional(),
        fieldWithPath("settings").type(JsonFieldType.OBJECT)
            .description("워크스페이스 설정 (null인 경우 기본값 사용)").optional(),
        fieldWithPath("settings.theme").type(JsonFieldType.STRING)
            .description("테마 설정 (light, dark)").optional(),
        fieldWithPath("settings.language").type(JsonFieldType.STRING)
            .description("언어 설정 (ko, en)").optional());
  }

  /** 워크스페이스 수정 응답 헤더 */
  public static Snippet updateWorkspaceResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 워크스페이스 수정 응답 */
  public static Snippet updateWorkspaceResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(workspaceResponseFields()));
  }

  // ========== DELETE /api/workspaces/{id} - 워크스페이스 삭제 ==========

  /** 워크스페이스 삭제 경로 파라미터 */
  public static Snippet deleteWorkspacePathParameters() {
    return pathParameters(
        parameterWithName("id").description("워크스페이스 ID (ULID)"));
  }

  /** 워크스페이스 삭제 요청 헤더 */
  public static Snippet deleteWorkspaceRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 워크스페이스 삭제 응답 */
  public static Snippet deleteWorkspaceResponse() {
    return createResponseFieldsSnippet(
        successResponseFieldsWithNullResult());
  }

  // ========== GET /api/workspaces/{id}/members - 워크스페이스 멤버 조회 ==========

  /** 워크스페이스 멤버 조회 경로 파라미터 */
  public static Snippet getWorkspaceMembersPathParameters() {
    return pathParameters(
        parameterWithName("id").description("워크스페이스 ID (ULID)"));
  }

  /** 워크스페이스 멤버 조회 요청 헤더 */
  public static Snippet getWorkspaceMembersRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 워크스페이스 멤버 조회 쿼리 파라미터 */
  public static Snippet getWorkspaceMembersQueryParameters() {
    return queryParameters(
        parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)")
            .optional(),
        parameterWithName("size")
            .description("페이지 크기 (기본값: 20, 최대: 100)").optional());
  }

  /** 워크스페이스 멤버 조회 응답 헤더 */
  public static Snippet getWorkspaceMembersResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 워크스페이스 멤버 조회 응답 */
  public static Snippet getWorkspaceMembersResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            fieldWithPath("content[]")
                .type(JsonFieldType.ARRAY)
                .description("멤버 목록"),
            fieldWithPath("content[].id")
                .type(JsonFieldType.STRING)
                .description("멤버십 ID (ULID)"),
            fieldWithPath("content[].userId")
                .type(JsonFieldType.STRING)
                .description("사용자 ID (ULID)"),
            fieldWithPath("content[].userName")
                .type(JsonFieldType.STRING)
                .description("사용자 이름"),
            fieldWithPath("content[].userEmail")
                .type(JsonFieldType.STRING)
                .description("사용자 이메일"),
            fieldWithPath("content[].role")
                .type(JsonFieldType.STRING)
                .description(
                    "워크스페이스 내 역할 (OWNER, ADMIN, MEMBER)"),
            fieldWithPath("content[].joinedAt")
                .type(JsonFieldType.STRING)
                .description("가입 시각 (ISO 8601)"),
            fieldWithPath("page").type(JsonFieldType.NUMBER)
                .description("현재 페이지 번호 (0부터 시작)"),
            fieldWithPath("size").type(JsonFieldType.NUMBER)
                .description("페이지 크기"),
            fieldWithPath("totalElements")
                .type(JsonFieldType.NUMBER)
                .description("전체 멤버 수"),
            fieldWithPath("totalPages")
                .type(JsonFieldType.NUMBER)
                .description("전체 페이지 수")));
  }

  // ========== POST /api/workspaces/{workspaceId}/members - 멤버 추가 ==========

  /** 멤버 추가 경로 파라미터 */
  public static Snippet addMemberPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID (ULID)"));
  }

  /** 멤버 추가 요청 헤더 */
  public static Snippet addMemberRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 멤버 추가 요청 바디 */
  public static Snippet addMemberRequest() {
    return requestFields(
        fieldWithPath("userId").type(JsonFieldType.STRING)
            .description("추가할 사용자의 ID (필수)"),
        fieldWithPath("role").type(JsonFieldType.STRING)
            .description("부여할 역할 (ADMIN, MEMBER)"));
  }

  /** 멤버 추가 응답 헤더 */
  public static Snippet addMemberResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 멤버 추가 응답 */
  public static Snippet addMemberResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            fieldWithPath("id").type(JsonFieldType.STRING)
                .description("생성된 멤버십 ID (ULID)"),
            fieldWithPath("userId")
                .type(JsonFieldType.STRING)
                .description("사용자 ID (ULID)"),
            fieldWithPath("userName")
                .type(JsonFieldType.STRING)
                .description("사용자 이름"),
            fieldWithPath("userEmail")
                .type(JsonFieldType.STRING)
                .description("사용자 이메일"),
            fieldWithPath("role").type(JsonFieldType.STRING)
                .description("할당된 역할"),
            fieldWithPath("joinedAt")
                .type(JsonFieldType.STRING)
                .description("가입 시각 (ISO 8601)")));
  }

  // ========== DELETE /api/workspaces/{workspaceId}/members/{memberId} - 멤버 추방 ==========

  /** 멤버 추방 경로 파라미터 */
  public static Snippet removeMemberPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID (ULID)"),
        parameterWithName("memberId").description("멤버십 ID (ULID)"));
  }

  /** 멤버 추방 요청 헤더 */
  public static Snippet removeMemberRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  // ========== DELETE /api/workspaces/{workspaceId}/members/me - 워크스페이스 탈퇴 ==========

  /** 워크스페이스 탈퇴 경로 파라미터 */
  public static Snippet leaveMemberPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID (ULID)"));
  }

  /** 워크스페이스 탈퇴 요청 헤더 */
  public static Snippet leaveMemberRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

}
