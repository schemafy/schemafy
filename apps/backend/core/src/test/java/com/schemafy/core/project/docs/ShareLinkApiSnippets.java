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

/**
 * ShareLink API 문서화를 위한 스니펫 제공 클래스
 *
 * <p>비즈니스 규칙 & 보안 정책:</p>
 * <ul>
 *   <li>공유 링크는 토큰 기반으로 프로젝트에 접근 권한 부여</li>
 *   <li>공유 링크에는 만료 시간 설정 가능 (expiresAt)</li>
 *   <li>공유 링크는 revoke 가능 (isRevoked)</li>
 *   <li>공유 링크는 역할(VIEWER, COMMENTER, EDITOR) 지정 가능</li>
 *   <li>공유 링크 생성/조회/삭제/비활성화는 프로젝트 멤버만 가능</li>
 * </ul>
 */
public class ShareLinkApiSnippets extends RestDocsSnippets {

    // ========== ShareLink 도메인 공통 필드 ==========

    /**
     * ShareLink 응답 필드 (상세 정보)
     */
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

    /**
     * ShareLink 목록 응답 필드 (token 제외)
     */
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

    /**
     * 공유 링크 생성 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * POST /api/workspaces/{workspaceId}/projects/{projectId}/share-links
     * Authorization: Bearer {accessToken}
     *
     * {
     *   "role": "viewer",
     *   "expiresAt": "2025-12-31T23:59:59Z"
     * }
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>역할(role)은 필수: viewer, commenter, editor 중 하나</li>
     *   <li>만료 시간(expiresAt)은 선택 사항 (null이면 무제한)</li>
     *   <li>생성 시 고유 토큰이 자동 발급됨</li>
     *   <li>초기 상태는 활성(isRevoked=false)</li>
     *   <li>accessCount는 0으로 초기화</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>프로젝트 멤버만 생성 가능</li>
     *   <li>토큰은 암호화되어 저장됨</li>
     * </ul>
     */
    public static Snippet createShareLinkPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"),
                parameterWithName("projectId").description("프로젝트 ID (ULID)"));
    }

    /**
     * 공유 링크 생성 요청 헤더
     */
    public static Snippet createShareLinkRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 공유 링크 생성 요청 바디
     */
    public static Snippet createShareLinkRequest() {
        return requestFields(
                fieldWithPath("role").type(JsonFieldType.STRING)
                        .description("부여할 역할 (viewer, commenter, editor 중 하나, 필수)"),
                fieldWithPath("expiresAt").type(JsonFieldType.STRING)
                        .description("만료 시각 (ISO 8601 형식, 선택 사항)").optional());
    }

    /**
     * 공유 링크 생성 응답 헤더
     */
    public static Snippet createShareLinkResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 공유 링크 생성 응답
     */
    public static Snippet createShareLinkResponse() {
        return createResponseFieldsSnippet(successResponseFields(shareLinkResponseFields()));
    }

    // ========== GET /api/workspaces/{workspaceId}/projects/{projectId}/share-links - 공유 링크 목록 조회 ==========

    /**
     * 공유 링크 목록 조회 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * GET /api/workspaces/{workspaceId}/projects/{projectId}/share-links?page=0&size=20
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>프로젝트의 모든 공유 링크 조회</li>
     *   <li>페이징 지원 (기본값: page=0, size=20)</li>
     *   <li>비활성화된 링크도 포함</li>
     *   <li>보안상 토큰은 목록에서 제외됨</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>프로젝트 멤버만 조회 가능</li>
     *   <li>토큰 정보는 상세 조회에서만 제공</li>
     * </ul>
     */
    public static Snippet getShareLinksPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"),
                parameterWithName("projectId").description("프로젝트 ID (ULID)"));
    }

    /**
     * 공유 링크 목록 조회 요청 헤더
     */
    public static Snippet getShareLinksRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 공유 링크 목록 조회 쿼리 파라미터
     */
    public static Snippet getShareLinksQueryParameters() {
        return queryParameters(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20, 최대: 100)").optional());
    }

    /**
     * 공유 링크 목록 조회 응답 헤더
     */
    public static Snippet getShareLinksResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 공유 링크 목록 조회 응답
     */
    public static Snippet getShareLinksResponse() {
        return createResponseFieldsSnippet(
                successResponseFields(concat(
                        new FieldDescriptor[] {
                            fieldWithPath("result.content[]").type(JsonFieldType.ARRAY)
                                    .description("공유 링크 목록"),
                            fieldWithPath("result.page").type(JsonFieldType.NUMBER)
                                    .description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("result.size").type(JsonFieldType.NUMBER)
                                    .description("페이지 크기"),
                            fieldWithPath("result.totalElements").type(JsonFieldType.NUMBER)
                                    .description("전체 공유 링크 개수"),
                            fieldWithPath("result.totalPages").type(JsonFieldType.NUMBER)
                                    .description("전체 페이지 수")
                        },
                        shareLinkSummaryFields("result.content[].")
                )));
    }

    // ========== GET /api/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId} - 공유 링크 상세 조회 ==========

    /**
     * 공유 링크 상세 조회 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * GET /api/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>특정 공유 링크의 상세 정보 조회</li>
     *   <li>토큰 정보 포함 (암호화된 형태)</li>
     *   <li>접근 통계 포함 (lastAccessedAt, accessCount)</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>프로젝트 멤버만 조회 가능</li>
     *   <li>토큰은 암호화된 형태로 반환</li>
     * </ul>
     */
    public static Snippet getShareLinkPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"),
                parameterWithName("projectId").description("프로젝트 ID (ULID)"),
                parameterWithName("shareLinkId").description("공유 링크 ID (ULID)"));
    }

    /**
     * 공유 링크 상세 조회 요청 헤더
     */
    public static Snippet getShareLinkRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 공유 링크 상세 조회 응답 헤더
     */
    public static Snippet getShareLinkResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 공유 링크 상세 조회 응답
     */
    public static Snippet getShareLinkResponse() {
        return createResponseFieldsSnippet(successResponseFields(shareLinkResponseFields()));
    }

    // ========== PATCH /api/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}/revoke - 공유 링크 비활성화 ==========

    /**
     * 공유 링크 비활성화 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * PATCH /api/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}/revoke
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>공유 링크를 비활성화(revoke)함</li>
     *   <li>isRevoked를 true로 설정</li>
     *   <li>비활성화된 링크는 더 이상 접근 불가</li>
     *   <li>삭제와 달리 이력은 보존됨</li>
     *   <li>재활성화는 불가능 (새로 생성해야 함)</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>프로젝트 멤버만 비활성화 가능</li>
     *   <li>비활성화 즉시 토큰 접근이 차단됨</li>
     * </ul>
     */
    public static Snippet revokeShareLinkPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"),
                parameterWithName("projectId").description("프로젝트 ID (ULID)"),
                parameterWithName("shareLinkId").description("공유 링크 ID (ULID)"));
    }

    /**
     * 공유 링크 비활성화 요청 헤더
     */
    public static Snippet revokeShareLinkRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 공유 링크 비활성화 응답 헤더
     */
    public static Snippet revokeShareLinkResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 공유 링크 비활성화 응답
     */
    public static Snippet revokeShareLinkResponse() {
        return createResponseFieldsSnippet(successResponseFields(shareLinkResponseFields()));
    }

    // ========== DELETE /api/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId} - 공유 링크 삭제 ==========

    /**
     * 공유 링크 삭제 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * DELETE /api/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>공유 링크를 완전히 삭제</li>
     *   <li>이력이 모두 제거됨</li>
     *   <li>복구 불가능</li>
     *   <li>일반적으로 비활성화(revoke)를 권장</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>프로젝트 멤버만 삭제 가능</li>
     *   <li>삭제 즉시 토큰 접근이 차단됨</li>
     * </ul>
     */
    public static Snippet deleteShareLinkPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"),
                parameterWithName("projectId").description("프로젝트 ID (ULID)"),
                parameterWithName("shareLinkId").description("공유 링크 ID (ULID)"));
    }

    /**
     * 공유 링크 삭제 요청 헤더
     */
    public static Snippet deleteShareLinkRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 공유 링크 삭제 응답
     */
    public static Snippet deleteShareLinkResponse() {
        return createResponseFieldsSnippet(successResponseFieldsWithNullResult());
    }

}