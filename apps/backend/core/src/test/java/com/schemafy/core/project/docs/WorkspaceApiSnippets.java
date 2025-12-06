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
 * Workspace API 문서화를 위한 스니펫 제공 클래스
 *
 * <p>비즈니스 규칙 & 보안 정책:</p>
 * <ul>
 *   <li>워크스페이스 생성 시 요청자가 자동으로 OWNER가 됨</li>
 *   <li>워크스페이스 조회는 멤버(OWNER, ADMIN, MEMBER)만 가능</li>
 *   <li>워크스페이스 수정/삭제는 OWNER 또는 ADMIN만 가능</li>
 *   <li>멤버 목록 조회는 워크스페이스 멤버만 가능</li>
 * </ul>
 */
public class WorkspaceApiSnippets extends RestDocsSnippets {

    // ========== Workspace 도메인 공통 필드 ==========

    /**
     * WorkspaceSettings 응답 필드
     */
    private static FieldDescriptor[] workspaceSettingsFields(String prefix) {
        return new FieldDescriptor[] {
            fieldWithPath(prefix + "theme").type(JsonFieldType.STRING)
                    .description("테마 설정 (light, dark)"),
            fieldWithPath(prefix + "language").type(JsonFieldType.STRING)
                    .description("언어 설정 (ko, en)"),
            fieldWithPath(prefix + "defaultProjectAccess").type(JsonFieldType.STRING)
                    .description("기본 프로젝트 접근 권한 (viewer, editor)")
        };
    }

    /**
     * Workspace 응답 필드 (상세 정보)
     */
    private static FieldDescriptor[] workspaceResponseFields() {
        return concat(
            new FieldDescriptor[] {
                fieldWithPath("result.id").type(JsonFieldType.STRING)
                        .description("워크스페이스 고유 ID (ULID)"),
                fieldWithPath("result.name").type(JsonFieldType.STRING)
                        .description("워크스페이스 이름 (1-255자)"),
                fieldWithPath("result.description").type(JsonFieldType.STRING)
                        .description("워크스페이스 설명 (최대 1000자)").optional(),
                fieldWithPath("result.ownerId").type(JsonFieldType.STRING)
                        .description("워크스페이스 소유자 ID (ULID)"),
                fieldWithPath("result.settings").type(JsonFieldType.OBJECT)
                        .description("워크스페이스 설정"),
                fieldWithPath("result.createdAt").type(JsonFieldType.STRING)
                        .description("생성 시각 (ISO 8601)"),
                fieldWithPath("result.updatedAt").type(JsonFieldType.STRING)
                        .description("수정 시각 (ISO 8601)")
            },
            workspaceSettingsFields("result.settings.")
        );
    }

    /**
     * WorkspaceSummary 응답 필드 (목록 조회용)
     */
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
            fieldWithPath(prefix + "role").type(JsonFieldType.STRING)
                    .description("현재 사용자의 역할 (OWNER, ADMIN, MEMBER)"),
            fieldWithPath(prefix + "memberCount").type(JsonFieldType.NUMBER)
                    .description("전체 멤버 수"),
            fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING)
                    .description("생성 시각 (ISO 8601)"),
            fieldWithPath(prefix + "updatedAt").type(JsonFieldType.STRING)
                    .description("수정 시각 (ISO 8601)")
        };
    }

    // ========== POST /api/workspaces - 워크스페이스 생성 ==========

    /**
     * 워크스페이스 생성 요청 바디
     *
     * <p>사용 예시:</p>
     * <pre>
     * POST /api/workspaces
     * Authorization: Bearer {accessToken}
     *
     * {
     *   "name": "My Workspace",
     *   "description": "개발팀 워크스페이스",
     *   "settings": {
     *     "theme": "light",
     *     "language": "ko",
     *     "defaultProjectAccess": "viewer"
     *   }
     * }
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>워크스페이스 이름은 1-255자 필수</li>
     *   <li>설명은 최대 1000자</li>
     *   <li>생성자가 자동으로 OWNER 역할로 등록됨</li>
     *   <li>settings가 null이면 기본값 사용</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>인증된 사용자만 생성 가능</li>
     * </ul>
     */
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
                        .description("언어 설정 (ko, en)").optional(),
                fieldWithPath("settings.defaultProjectAccess").type(JsonFieldType.STRING)
                        .description("기본 프로젝트 접근 권한 (viewer, editor)").optional());
    }

    /**
     * 워크스페이스 생성 요청 헤더
     */
    public static Snippet createWorkspaceRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 워크스페이스 생성 응답 헤더
     */
    public static Snippet createWorkspaceResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 워크스페이스 생성 응답
     */
    public static Snippet createWorkspaceResponse() {
        return createResponseFieldsSnippet(successResponseFields(workspaceResponseFields()));
    }

    // ========== GET /api/workspaces - 워크스페이스 목록 조회 ==========

    /**
     * 워크스페이스 목록 조회 요청 헤더
     *
     * <p>사용 예시:</p>
     * <pre>
     * GET /api/workspaces?page=0&size=20
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>현재 사용자가 멤버로 속한 워크스페이스만 조회</li>
     *   <li>페이징 지원 (기본값: page=0, size=20)</li>
     *   <li>역할 정보 및 멤버 수 포함</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>본인이 속한 워크스페이스만 조회 가능</li>
     * </ul>
     */
    public static Snippet getWorkspacesRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 워크스페이스 목록 조회 쿼리 파라미터
     */
    public static Snippet getWorkspacesQueryParameters() {
        return queryParameters(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20, 최대: 100)").optional());
    }

    /**
     * 워크스페이스 목록 조회 응답 헤더
     */
    public static Snippet getWorkspacesResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 워크스페이스 목록 조회 응답
     */
    public static Snippet getWorkspacesResponse() {
        return createResponseFieldsSnippet(
                successResponseFields(concat(
                        new FieldDescriptor[] {
                            fieldWithPath("result.content[]").type(JsonFieldType.ARRAY)
                                    .description("워크스페이스 목록"),
                            fieldWithPath("result.page").type(JsonFieldType.NUMBER)
                                    .description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("result.size").type(JsonFieldType.NUMBER)
                                    .description("페이지 크기"),
                            fieldWithPath("result.totalElements").type(JsonFieldType.NUMBER)
                                    .description("전체 워크스페이스 개수"),
                            fieldWithPath("result.totalPages").type(JsonFieldType.NUMBER)
                                    .description("전체 페이지 수")
                        },
                        workspaceSummaryFields("result.content[].")
                )));
    }

    // ========== GET /api/workspaces/{id} - 워크스페이스 상세 조회 ==========

    /**
     * 워크스페이스 상세 조회 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * GET /api/workspaces/{id}
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>워크스페이스 멤버만 조회 가능</li>
     *   <li>설정 정보 포함</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>워크스페이스 멤버(OWNER, ADMIN, MEMBER)만 조회 가능</li>
     * </ul>
     */
    public static Snippet getWorkspacePathParameters() {
        return pathParameters(
                parameterWithName("id").description("워크스페이스 ID (ULID)"));
    }

    /**
     * 워크스페이스 상세 조회 요청 헤더
     */
    public static Snippet getWorkspaceRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 워크스페이스 상세 조회 응답 헤더
     */
    public static Snippet getWorkspaceResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 워크스페이스 상세 조회 응답
     */
    public static Snippet getWorkspaceResponse() {
        return createResponseFieldsSnippet(successResponseFields(workspaceResponseFields()));
    }

    // ========== PUT /api/workspaces/{id} - 워크스페이스 수정 ==========

    /**
     * 워크스페이스 수정 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * PUT /api/workspaces/{id}
     * Authorization: Bearer {accessToken}
     *
     * {
     *   "name": "Updated Workspace",
     *   "description": "수정된 설명",
     *   "settings": {
     *     "theme": "dark",
     *     "language": "en",
     *     "defaultProjectAccess": "editor"
     *   }
     * }
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>워크스페이스 이름은 1-255자 필수</li>
     *   <li>설명은 최대 1000자</li>
     *   <li>settings가 null이면 기본값 사용</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>OWNER 또는 ADMIN만 수정 가능</li>
     * </ul>
     */
    public static Snippet updateWorkspacePathParameters() {
        return pathParameters(
                parameterWithName("id").description("워크스페이스 ID (ULID)"));
    }

    /**
     * 워크스페이스 수정 요청 헤더
     */
    public static Snippet updateWorkspaceRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 워크스페이스 수정 요청 바디
     */
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
                        .description("언어 설정 (ko, en)").optional(),
                fieldWithPath("settings.defaultProjectAccess").type(JsonFieldType.STRING)
                        .description("기본 프로젝트 접근 권한 (viewer, editor)").optional());
    }

    /**
     * 워크스페이스 수정 응답 헤더
     */
    public static Snippet updateWorkspaceResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 워크스페이스 수정 응답
     */
    public static Snippet updateWorkspaceResponse() {
        return createResponseFieldsSnippet(successResponseFields(workspaceResponseFields()));
    }

    // ========== DELETE /api/workspaces/{id} - 워크스페이스 삭제 ==========

    /**
     * 워크스페이스 삭제 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * DELETE /api/workspaces/{id}
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>워크스페이스 삭제 시 하위 프로젝트도 모두 삭제됨</li>
     *   <li>복구 불가능</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>OWNER 또는 ADMIN만 삭제 가능</li>
     * </ul>
     */
    public static Snippet deleteWorkspacePathParameters() {
        return pathParameters(
                parameterWithName("id").description("워크스페이스 ID (ULID)"));
    }

    /**
     * 워크스페이스 삭제 요청 헤더
     */
    public static Snippet deleteWorkspaceRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 워크스페이스 삭제 응답
     */
    public static Snippet deleteWorkspaceResponse() {
        return createResponseFieldsSnippet(successResponseFieldsWithNullResult());
    }

    // ========== GET /api/workspaces/{id}/members - 워크스페이스 멤버 조회 ==========

    /**
     * 워크스페이스 멤버 조회 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * GET /api/workspaces/{id}/members?page=0&size=20
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>워크스페이스 멤버 목록 조회</li>
     *   <li>페이징 지원 (기본값: page=0, size=20)</li>
     *   <li>사용자 정보 및 역할 포함</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>워크스페이스 멤버만 조회 가능</li>
     * </ul>
     */
    public static Snippet getWorkspaceMembersPathParameters() {
        return pathParameters(
                parameterWithName("id").description("워크스페이스 ID (ULID)"));
    }

    /**
     * 워크스페이스 멤버 조회 요청 헤더
     */
    public static Snippet getWorkspaceMembersRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 워크스페이스 멤버 조회 쿼리 파라미터
     */
    public static Snippet getWorkspaceMembersQueryParameters() {
        return queryParameters(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20, 최대: 100)").optional());
    }

    /**
     * 워크스페이스 멤버 조회 응답 헤더
     */
    public static Snippet getWorkspaceMembersResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 워크스페이스 멤버 조회 응답
     */
    public static Snippet getWorkspaceMembersResponse() {
        return createResponseFieldsSnippet(
                successResponseFields(
                        fieldWithPath("result.content[]").type(JsonFieldType.ARRAY)
                                .description("멤버 목록"),
                        fieldWithPath("result.content[].id").type(JsonFieldType.STRING)
                                .description("멤버십 ID (ULID)"),
                        fieldWithPath("result.content[].userId").type(JsonFieldType.STRING)
                                .description("사용자 ID (ULID)"),
                        fieldWithPath("result.content[].userName").type(JsonFieldType.STRING)
                                .description("사용자 이름"),
                        fieldWithPath("result.content[].userEmail").type(JsonFieldType.STRING)
                                .description("사용자 이메일"),
                        fieldWithPath("result.content[].role").type(JsonFieldType.STRING)
                                .description("워크스페이스 내 역할 (OWNER, ADMIN, MEMBER)"),
                        fieldWithPath("result.content[].joinedAt").type(JsonFieldType.STRING)
                                .description("가입 시각 (ISO 8601)"),
                        fieldWithPath("result.page").type(JsonFieldType.NUMBER)
                                .description("현재 페이지 번호 (0부터 시작)"),
                        fieldWithPath("result.size").type(JsonFieldType.NUMBER)
                                .description("페이지 크기"),
                        fieldWithPath("result.totalElements").type(JsonFieldType.NUMBER)
                                .description("전체 멤버 수"),
                        fieldWithPath("result.totalPages").type(JsonFieldType.NUMBER)
                                .description("전체 페이지 수")));
    }

}