package com.schemafy.core.project.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

/** ProjectInvitation API 문서화를 위한 스니펫 제공 클래스 */
public class ProjectInvitationApiSnippets extends RestDocsSnippets {

  // ========== 공통 필드 ==========

  /** ProjectInvitation 응답 필드 */
  private static FieldDescriptor[] invitationResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("초대 고유 ID"),
      fieldWithPath(prefix + "workspaceId").type(JsonFieldType.STRING)
          .description("워크스페이스 ID"),
      fieldWithPath(prefix + "projectId").type(JsonFieldType.STRING)
          .description("프로젝트 ID"),
      fieldWithPath(prefix + "invitedEmail").type(JsonFieldType.STRING)
          .description("초대된 사용자 이메일"),
      fieldWithPath(prefix + "invitedRole").type(JsonFieldType.STRING)
          .description("초대된 역할 (admin/editor/viewer)"),
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

  // ========== GET /api/users/me/invitations/projects - 내 프로젝트 초대 목록 조회 ==========

  /** 내 초대 목록 조회 요청 헤더 */
  public static Snippet listMyInvitationsRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 내 초대 목록 조회 쿼리 파라미터 */
  public static Snippet listMyInvitationsQueryParameters() {
    return queryParameters(
        parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
        parameterWithName("size").description("페이지 크기 (기본값: 20)").optional());
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

}
