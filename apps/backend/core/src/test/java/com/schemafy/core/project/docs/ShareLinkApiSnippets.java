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

/** ShareLink API 문서화를 위한 스니펫 제공 클래스 */
public class ShareLinkApiSnippets extends RestDocsSnippets {

  // ========== ShareLink 도메인 공통 필드 ==========

  /** ShareLink 응답 필드 (상세 정보) */
  private static FieldDescriptor[] shareLinkResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result.id").type(JsonFieldType.STRING)
          .description("공유 링크 고유 ID (ULID)"),
      fieldWithPath("result.projectId").type(JsonFieldType.STRING)
          .description("대상 프로젝트 ID (ULID)"),
      fieldWithPath("result.token").type(JsonFieldType.STRING)
          .description("공유 토큰 (암호화된 접근 키)").optional(),
      fieldWithPath("result.role").type(JsonFieldType.STRING)
          .description("부여할 역할 (viewer, commenter, editor)"),
      fieldWithPath("result.expiresAt").type(JsonFieldType.STRING)
          .description("만료 시각 (ISO 8601)").optional(),
      fieldWithPath("result.isRevoked").type(JsonFieldType.BOOLEAN)
          .description("비활성화 여부"),
      fieldWithPath("result.lastAccessedAt").type(JsonFieldType.STRING)
          .description("마지막 접근 시각 (ISO 8601)").optional(),
      fieldWithPath("result.accessCount").type(JsonFieldType.NUMBER)
          .description("총 접근 횟수"),
      fieldWithPath("result.createdAt").type(JsonFieldType.STRING)
          .description("생성 시각 (ISO 8601)")
    };
  }

  /** ShareLink 목록 응답 필드 (token 제외) */
  private static FieldDescriptor[] shareLinkSummaryFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("공유 링크 고유 ID (ULID)"),
      fieldWithPath(prefix + "projectId").type(JsonFieldType.STRING)
          .description("대상 프로젝트 ID (ULID)"),
      fieldWithPath(prefix + "role").type(JsonFieldType.STRING)
          .description("부여할 역할 (viewer, commenter, editor)"),
      fieldWithPath(prefix + "expiresAt").type(JsonFieldType.STRING)
          .description("만료 시각 (ISO 8601)").optional(),
      fieldWithPath(prefix + "isRevoked").type(JsonFieldType.BOOLEAN)
          .description("비활성화 여부"),
      fieldWithPath(prefix + "lastAccessedAt").type(JsonFieldType.STRING)
          .description("마지막 접근 시각 (ISO 8601)").optional(),
      fieldWithPath(prefix + "accessCount").type(JsonFieldType.NUMBER)
          .description("총 접근 횟수"),
      fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING)
          .description("생성 시각 (ISO 8601)")
    };
  }

  // ========== POST /api/workspaces/{workspaceId}/projects/{projectId}/share-links - 공유 링크 생성 ==========

  /** 공유 링크 생성 경로 파라미터 */
  public static Snippet createShareLinkPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID (ULID)"),
        parameterWithName("projectId").description("프로젝트 ID (ULID)"));
  }

  /** 공유 링크 생성 요청 헤더 */
  public static Snippet createShareLinkRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 공유 링크 생성 요청 바디 */
  public static Snippet createShareLinkRequest() {
    return requestFields(
        fieldWithPath("role").type(JsonFieldType.STRING)
            .description(
                "부여할 역할 (viewer, commenter, editor 중 하나, 필수)"),
        fieldWithPath("expiresAt").type(JsonFieldType.STRING)
            .description("만료 시각 (ISO 8601 형식, 선택 사항)").optional());
  }

  /** 공유 링크 생성 응답 헤더 */
  public static Snippet createShareLinkResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 공유 링크 생성 응답 */
  public static Snippet createShareLinkResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(shareLinkResponseFields()));
  }

  // ========== GET /api/workspaces/{workspaceId}/projects/{projectId}/share-links - 공유 링크 목록 조회 ==========

  /** 공유 링크 목록 조회 경로 파라미터 */
  public static Snippet getShareLinksPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID (ULID)"),
        parameterWithName("projectId").description("프로젝트 ID (ULID)"));
  }

  /** 공유 링크 목록 조회 요청 헤더 */
  public static Snippet getShareLinksRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 공유 링크 목록 조회 쿼리 파라미터 */
  public static Snippet getShareLinksQueryParameters() {
    return queryParameters(
        parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)")
            .optional(),
        parameterWithName("size")
            .description("페이지 크기 (기본값: 20, 최대: 100)").optional());
  }

  /** 공유 링크 목록 조회 응답 헤더 */
  public static Snippet getShareLinksResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 공유 링크 목록 조회 응답 */
  public static Snippet getShareLinksResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(concat(
            new FieldDescriptor[] {
              fieldWithPath("result.content[]")
                  .type(JsonFieldType.ARRAY)
                  .description("공유 링크 목록"),
              fieldWithPath("result.page")
                  .type(JsonFieldType.NUMBER)
                  .description("현재 페이지 번호 (0부터 시작)"),
              fieldWithPath("result.size")
                  .type(JsonFieldType.NUMBER)
                  .description("페이지 크기"),
              fieldWithPath("result.totalElements")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 공유 링크 개수"),
              fieldWithPath("result.totalPages")
                  .type(JsonFieldType.NUMBER)
                  .description("전체 페이지 수")
            },
            shareLinkSummaryFields("result.content[]."))));
  }

  // ========== GET /api/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId} - 공유 링크 상세 조회
  // ==========

  /** 공유 링크 상세 조회 경로 파라미터 */
  public static Snippet getShareLinkPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID (ULID)"),
        parameterWithName("projectId").description("프로젝트 ID (ULID)"),
        parameterWithName("shareLinkId")
            .description("공유 링크 ID (ULID)"));
  }

  /** 공유 링크 상세 조회 요청 헤더 */
  public static Snippet getShareLinkRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 공유 링크 상세 조회 응답 헤더 */
  public static Snippet getShareLinkResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 공유 링크 상세 조회 응답 */
  public static Snippet getShareLinkResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(shareLinkResponseFields()));
  }

  // ========== PATCH /api/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}/revoke - 공유 링크 비활성화
  // ==========

  /** 공유 링크 비활성화 경로 파라미터 */
  public static Snippet revokeShareLinkPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID (ULID)"),
        parameterWithName("projectId").description("프로젝트 ID (ULID)"),
        parameterWithName("shareLinkId")
            .description("공유 링크 ID (ULID)"));
  }

  /** 공유 링크 비활성화 요청 헤더 */
  public static Snippet revokeShareLinkRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 공유 링크 비활성화 응답 헤더 */
  public static Snippet revokeShareLinkResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 공유 링크 비활성화 응답 */
  public static Snippet revokeShareLinkResponse() {
    return createResponseFieldsSnippet(
        fieldWithPath("success").type(JsonFieldType.BOOLEAN)
            .description("비활성화 성공 여부"));
  }

  // ========== DELETE /api/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId} - 공유 링크 삭제
  // ==========

  /** 공유 링크 삭제 경로 파라미터 */
  public static Snippet deleteShareLinkPathParameters() {
    return pathParameters(
        parameterWithName("workspaceId")
            .description("워크스페이스 ID (ULID)"),
        parameterWithName("projectId").description("프로젝트 ID (ULID)"),
        parameterWithName("shareLinkId")
            .description("공유 링크 ID (ULID)"));
  }

  /** 공유 링크 삭제 요청 헤더 */
  public static Snippet deleteShareLinkRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  /** 공유 링크 삭제 응답 */
  public static Snippet deleteShareLinkResponse() {
    return createResponseFieldsSnippet(
        successResponseFieldsWithNullResult());
  }

  // ========== GET /api/public/share/{token} - 공유 링크 공개 접근 ==========

  /** 공유 링크 공개 접근 경로 파라미터 */
  public static Snippet accessShareLinkPublicPathParameters() {
    return pathParameters(
        parameterWithName("token")
            .description("공유 링크 토큰 (URL-safe Base64 인코딩)"));
  }

  /** 공유 링크 공개 접근 응답 헤더 */
  public static Snippet accessShareLinkPublicResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 공유 링크 공개 접근 응답 */
  public static Snippet accessShareLinkPublicResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            fieldWithPath("result.projectId").type(JsonFieldType.STRING)
                .description("프로젝트 고유 ID (ULID)"),
            fieldWithPath("result.projectName")
                .type(JsonFieldType.STRING)
                .description("프로젝트 이름"),
            fieldWithPath("result.description")
                .type(JsonFieldType.STRING)
                .description("프로젝트 설명").optional(),
            fieldWithPath("result.settings").type(JsonFieldType.OBJECT)
                .description("프로젝트 설정"),
            fieldWithPath("result.settings.theme")
                .type(JsonFieldType.STRING)
                .description("테마 설정 (예: light, dark)"),
            fieldWithPath("result.settings.language")
                .type(JsonFieldType.STRING)
                .description("언어 설정 (예: ko, en)"),
            fieldWithPath("result.grantedRole")
                .type(JsonFieldType.STRING)
                .description(
                    "부여된 역할 (viewer, commenter, editor). 익명 사용자는 항상 viewer"),
            fieldWithPath("result.canEdit").type(JsonFieldType.BOOLEAN)
                .description("편집 권한 여부 (EDITOR 역할만 true)"),
            fieldWithPath("result.canComment")
                .type(JsonFieldType.BOOLEAN)
                .description(
                    "코멘트 권한 여부 (COMMENTER, EDITOR 역할일 때 true)")));
  }

}
