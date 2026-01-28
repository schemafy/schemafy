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

/** Invitation API 문서화를 위한 스니펫 제공 클래스 */
public class WorkspaceInvitationApiSnippets extends RestDocsSnippets {

  // ========== 공통 필드 ==========

  /** Invitation 생성 응답 필드 (WorkspaceInvitationCreateResponse) */
  private static FieldDescriptor[] invitationCreateResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "invitationId").type(JsonFieldType.STRING)
          .description("초대 고유 ID"),
      fieldWithPath(prefix + "workspaceId").type(JsonFieldType.STRING)
          .description("워크스페이스 ID"),
      fieldWithPath(prefix + "invitedEmail").type(JsonFieldType.STRING)
          .description("초대된 사용자 이메일"),
      fieldWithPath(prefix + "invitedRole").type(JsonFieldType.STRING)
          .description("초대된 역할 (admin/member)"),
      fieldWithPath(prefix + "invitedBy").type(JsonFieldType.STRING)
          .description("초대한 사용자 ID"),
      fieldWithPath(prefix + "status").type(JsonFieldType.STRING)
          .description("초대 상태 (pending/accepted/rejected)"),
      fieldWithPath(prefix + "expiresAt").type(JsonFieldType.STRING)
          .description("초대 만료 시각"),
      fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING)
          .description("초대 생성 시각")
    };
  }

  /** Invitation 조회 응답 필드 (WorkspaceInvitationResponse) */
  private static FieldDescriptor[] invitationResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("초대 고유 ID"),
      fieldWithPath(prefix + "workspaceId").type(JsonFieldType.STRING)
          .description("워크스페이스 ID"),
      fieldWithPath(prefix + "invitedEmail").type(JsonFieldType.STRING)
          .description("초대된 사용자 이메일"),
      fieldWithPath(prefix + "invitedRole").type(JsonFieldType.STRING)
          .description("초대된 역할 (admin/member)"),
      fieldWithPath(prefix + "invitedBy").type(JsonFieldType.STRING)
          .description("초대한 사용자 ID"),
      fieldWithPath(prefix + "status").type(JsonFieldType.STRING)
          .description("초대 상태 (pending/accepted/rejected)"),
      fieldWithPath(prefix + "expiresAt").type(JsonFieldType.STRING)
          .description("초대 만료 시각"),
      fieldWithPath(prefix + "resolvedAt").type(JsonFieldType.STRING)
          .description("초대 처리 시각 (수락/거절 시각)").optional(),
      fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING)
          .description("초대 생성 시각")
    };
  }

  // ========== GET /api/users/me/invitations/workspaces - 내 워크스페이스 초대 목록 조회 ==========

  /** 내 초대 목록 조회 요청 헤더 */
  public static Snippet listMyInvitationsRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 내 초대 목록 조회 쿼리 파라미터 */
  public static Snippet listMyInvitationsQueryParameters() {
    return queryParameters(
        parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
        parameterWithName("size").description("페이지 크기 (기본값: 10)").optional());
  }

  /** 내 초대 목록 조회 응답 본문 */
  public static Snippet listMyInvitationsResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(concat(
            new FieldDescriptor[] {
              fieldWithPath("result.content[]")
                  .type(JsonFieldType.ARRAY)
                  .description("초대 목록"),
              fieldWithPath("result.page")
                  .type(JsonFieldType.NUMBER)
                  .description("현재 페이지 번호 (0부터 시작)"),
              fieldWithPath("result.size")
                  .type(JsonFieldType.NUMBER)
                  .description("페이지 크기"),
              fieldWithPath("result.totalElements")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 초대 개수"),
              fieldWithPath("result.totalPages")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 페이지 수")
            },
            invitationResponseFields("result.content[]."))));
  }

  /** 내 초대 목록 조회 응답 헤더 */
  public static Snippet listMyInvitationsResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  // ========== POST /api/workspaces/{workspaceId}/invitations - 초대 생성 ==========

  /** 초대 생성 요청 헤더 */
  public static Snippet createInvitationRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 초대 생성 경로 파라미터 */
  public static Snippet createInvitationPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId").description("워크스페이스 ID"));
  }

  /** 초대 생성 요청 본문 */
  public static Snippet createInvitationRequest() {
    return requestFields(
        fieldWithPath("email").type(JsonFieldType.STRING).description("초대할 사용자 이메일"),
        fieldWithPath("role").type(JsonFieldType.STRING).description("초대 역할 (admin/member)"));
  }

  /** 초대 생성 응답 본문 */
  public static Snippet createInvitationResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(invitationCreateResponseFields("result.")));
  }

  /** 초대 생성 응답 헤더 */
  public static Snippet createInvitationResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  // ========== GET /api/workspaces/{workspaceId}/invitations - 초대 목록 조회 ==========

  /** 초대 목록 조회 요청 헤더 */
  public static Snippet listInvitationsRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 초대 목록 조회 경로 파라미터 */
  public static Snippet listInvitationsPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId").description("워크스페이스 ID"));
  }

  /** 초대 목록 조회 쿼리 파라미터 */
  public static Snippet listInvitationsQueryParameters() {
    return queryParameters(
        parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
        parameterWithName("size").description("페이지 크기 (기본값: 10)").optional());
  }

  /** 초대 목록 조회 응답 본문 */
  public static Snippet listInvitationsResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(concat(
            new FieldDescriptor[] {
              fieldWithPath("result.content[]")
                  .type(JsonFieldType.ARRAY)
                  .description("초대 목록"),
              fieldWithPath("result.page")
                  .type(JsonFieldType.NUMBER)
                  .description("현재 페이지 번호 (0부터 시작)"),
              fieldWithPath("result.size")
                  .type(JsonFieldType.NUMBER)
                  .description("페이지 크기"),
              fieldWithPath("result.totalElements")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 초대 개수"),
              fieldWithPath("result.totalPages")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 페이지 수")
            },
            invitationResponseFields("result.content[]."))));
  }

  /** 초대 목록 조회 응답 헤더 */
  public static Snippet listInvitationsResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  // ========== PUT /api/workspaces/invitations/{invitationId}/accept - 초대 수락 ==========

  /** 초대 수락 요청 헤더 */
  public static Snippet acceptInvitationRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 초대 수락 경로 파라미터 */
  public static Snippet acceptInvitationPathParameters() {
    return pathParameters(
        parameterWithName("invitationId").description("초대 ID"));
  }

  /** 초대 수락 응답 본문 */
  public static Snippet acceptInvitationResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(new FieldDescriptor[] {
          fieldWithPath("result.workspaceId").type(JsonFieldType.STRING).description("워크스페이스 ID"),
          fieldWithPath("result.userId").type(JsonFieldType.STRING).description("사용자 ID"),
          fieldWithPath("result.userName").type(JsonFieldType.STRING).description("사용자 이름"),
          fieldWithPath("result.userEmail").type(JsonFieldType.STRING).description("사용자 이메일"),
          fieldWithPath("result.role").type(JsonFieldType.STRING).description("워크스페이스 역할"),
          fieldWithPath("result.joinedAt").type(JsonFieldType.STRING).description("멤버 가입 시각")
        }));
  }

  /** 초대 수락 응답 헤더 */
  public static Snippet acceptInvitationResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  // ========== PUT /api/workspaces/invitations/{invitationId}/reject - 초대 거절 ==========

  /** 초대 거절 요청 헤더 */
  public static Snippet rejectInvitationRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 초대 거절 경로 파라미터 */
  public static Snippet rejectInvitationPathParameters() {
    return pathParameters(
        parameterWithName("invitationId").description("초대 ID"));
  }

  /** 초대 거절 응답 헤더 */
  public static Snippet rejectInvitationResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

}
