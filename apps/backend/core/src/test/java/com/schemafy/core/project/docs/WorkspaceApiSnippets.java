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

  /** Workspace 응답 필드 (상세 정보) */
  private static FieldDescriptor[] workspaceResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result.id").type(JsonFieldType.STRING)
          .description("워크스페이스 고유 ID"),
      fieldWithPath("result.name").type(JsonFieldType.STRING)
          .description("워크스페이스 이름 (1-255자)"),
      fieldWithPath("result.description")
          .type(JsonFieldType.STRING)
          .description("워크스페이스 설명 (최대 1000자)").optional(),
      fieldWithPath("result.createdAt").type(JsonFieldType.STRING)
          .description("생성 시각"),
      fieldWithPath("result.updatedAt").type(JsonFieldType.STRING)
          .description("수정 시각"),
      fieldWithPath("result.memberCount").type(JsonFieldType.NUMBER)
          .description("워크스페이스 멤버 수").optional(),
      fieldWithPath("result.projectCount").type(JsonFieldType.NUMBER)
          .description("워크스페이스 프로젝트 수").optional(),
      fieldWithPath("result.currentUserRole").type(JsonFieldType.STRING)
          .description("요청자의 워크스페이스 역할 (admin/member)").optional()
    };
  }

  /** WorkspaceSummary 응답 필드 (목록 조회용) */
  private static FieldDescriptor[] workspaceSummaryFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("워크스페이스 고유 ID"),
      fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
          .description("워크스페이스 이름"),
      fieldWithPath(prefix + "description").type(JsonFieldType.STRING)
          .description("워크스페이스 설명").optional(),
      fieldWithPath(prefix + "memberCount").type(JsonFieldType.NUMBER)
          .description("전체 멤버 수"),
      fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING)
          .description("생성 시각"),
      fieldWithPath(prefix + "updatedAt").type(JsonFieldType.STRING)
          .description("수정 시각")
    };
  }

  // ========== POST /api/workspaces - 워크스페이스 생성 ==========

  /** 워크스페이스 생성 요청 바디 */
  public static Snippet createWorkspaceRequest() {
    return requestFields(
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("워크스페이스 이름 (1-255자, 필수)"),
        fieldWithPath("description").type(JsonFieldType.STRING)
            .description("워크스페이스 설명 (최대 1000자)").optional());
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
              fieldWithPath("result.content[]")
                  .type(JsonFieldType.ARRAY)
                  .description("워크스페이스 목록"),
              fieldWithPath("result.page")
                  .type(JsonFieldType.NUMBER)
                  .description("현재 페이지 번호 (0부터 시작)"),
              fieldWithPath("result.size")
                  .type(JsonFieldType.NUMBER)
                  .description("페이지 크기"),
              fieldWithPath("result.totalElements")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 워크스페이스 개수"),
              fieldWithPath("result.totalPages")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 페이지 수")
            },
            workspaceSummaryFields("result.content[]."))));
  }

  // ========== GET /api/workspaces/{id} - 워크스페이스 상세 조회 ==========

  /** 워크스페이스 상세 조회 경로 파라미터 */
  public static Snippet getWorkspacePathParameters() {
    return pathParameters(
        parameterWithName("id").description("워크스페이스 ID"));
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
        parameterWithName("id").description("워크스페이스 ID"));
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
            .description("워크스페이스 설명 (최대 1000자)").optional());
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
        parameterWithName("id").description("워크스페이스 ID"));
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
        parameterWithName("id").description("워크스페이스 ID"));
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
            fieldWithPath("result.content[]")
                .type(JsonFieldType.ARRAY)
                .description("멤버 목록"),
            fieldWithPath("result.content[].id")
                .type(JsonFieldType.STRING)
                .description("멤버십 ID"),
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
                    "워크스페이스 내 역할 (OWNER, ADMIN, MEMBER)"),
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

  // ========== POST /api/workspaces/{workspaceId}/members - 멤버 추가 ==========

  /** 멤버 추가 경로 파라미터 */
  public static Snippet addMemberPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"));
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
            fieldWithPath("result.id").type(JsonFieldType.STRING)
                .description("생성된 멤버십 ID"),
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
                .description("할당된 역할"),
            fieldWithPath("result.joinedAt")
                .type(JsonFieldType.STRING)
                .description("가입 시각")));
  }

  // ========== DELETE /api/workspaces/{workspaceId}/members/{memberId} - 멤버 추방 ==========

  /** 멤버 추방 경로 파라미터 */
  public static Snippet removeMemberPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"),
        parameterWithName("memberId").description("멤버십 ID"));
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
            .description("워크스페이스 ID"));
  }

  /** 워크스페이스 탈퇴 요청 헤더 */
  public static Snippet leaveMemberRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  // ========== PATCH /api/workspaces/{workspaceId}/members/{memberId}/role - 멤버 역할 변경 ==========

  /** 멤버 역할 변경 경로 파라미터 */
  public static Snippet updateMemberRolePathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID"),
        parameterWithName("memberId").description("멤버십 ID"));
  }

  /** 멤버 역할 변경 요청 헤더 */
  public static Snippet updateMemberRoleRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 멤버 역할 변경 요청 바디 */
  public static Snippet updateMemberRoleRequest() {
    return requestFields(
        fieldWithPath("role").type(JsonFieldType.STRING)
            .description("변경할 역할 (ADMIN, MEMBER)"));
  }

  /** 멤버 역할 변경 응답 헤더 */
  public static Snippet updateMemberRoleResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 멤버 역할 변경 응답 */
  public static Snippet updateMemberRoleResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            fieldWithPath("result.id").type(JsonFieldType.STRING)
                .description("멤버십 ID"),
            fieldWithPath("result.userId").type(JsonFieldType.STRING)
                .description("사용자 ID"),
            fieldWithPath("result.userName").type(JsonFieldType.STRING)
                .description("사용자 이름"),
            fieldWithPath("result.userEmail").type(JsonFieldType.STRING)
                .description("사용자 이메일"),
            fieldWithPath("result.role").type(JsonFieldType.STRING)
                .description("변경된 역할"),
            fieldWithPath("result.joinedAt").type(JsonFieldType.STRING)
                .description("가입 시각")));
  }

}
