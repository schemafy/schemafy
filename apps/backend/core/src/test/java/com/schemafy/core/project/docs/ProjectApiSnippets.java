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
 * Project API 문서화를 위한 스니펫 제공 클래스
 *
 * <p>비즈니스 규칙 & 보안 정책:</p>
 * <ul>
 *   <li>프로젝트 생성/수정/삭제는 워크스페이스의 OWNER/ADMIN만 가능</li>
 *   <li>프로젝트 조회는 워크스페이스 멤버 + 프로젝트 멤버(OWNER, ADMIN, EDITOR, COMMENTER, VIEWER) 모두 가능</li>
 *   <li>프로젝트는 워크스페이스 내에 종속됨</li>
 * </ul>
 */
public class ProjectApiSnippets extends RestDocsSnippets {

    // ========== Project 도메인 공통 필드 ==========

    /**
     * ProjectSettings 응답 필드
     */
    private static FieldDescriptor[] projectSettingsFields(String prefix) {
        return new FieldDescriptor[] {
            fieldWithPath(prefix + "theme").type(JsonFieldType.STRING)
                    .description("테마 설정 (예: light, dark)"),
            fieldWithPath(prefix + "language").type(JsonFieldType.STRING)
                    .description("언어 설정 (예: ko, en)"),
            fieldWithPath(prefix + "defaultView").type(JsonFieldType.STRING)
                    .description("기본 뷰 설정 (예: table, grid)")
        };
    }

    /**
     * Project 응답 필드 (상세 정보)
     */
    private static FieldDescriptor[] projectResponseFields() {
        return concat(
            new FieldDescriptor[] {
                fieldWithPath("result.id").type(JsonFieldType.STRING)
                        .description("프로젝트 고유 ID (ULID)"),
                fieldWithPath("result.workspaceId").type(JsonFieldType.STRING)
                        .description("소속 워크스페이스 ID (ULID)"),
                fieldWithPath("result.ownerId").type(JsonFieldType.STRING)
                        .description("프로젝트 소유자 ID (ULID)"),
                fieldWithPath("result.name").type(JsonFieldType.STRING)
                        .description("프로젝트 이름"),
                fieldWithPath("result.description").type(JsonFieldType.STRING)
                        .description("프로젝트 설명").optional(),
                fieldWithPath("result.settings").type(JsonFieldType.OBJECT)
                        .description("프로젝트 설정"),
                fieldWithPath("result.createdAt").type(JsonFieldType.STRING)
                        .description("생성 시각 (ISO 8601)"),
                fieldWithPath("result.updatedAt").type(JsonFieldType.STRING)
                        .description("수정 시각 (ISO 8601)")
            },
            projectSettingsFields("result.settings.")
        );
    }

    /**
     * ProjectSummary 응답 필드 (목록 조회용)
     */
    private static FieldDescriptor[] projectSummaryFields(String prefix) {
        return new FieldDescriptor[] {
            fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
                    .description("프로젝트 고유 ID (ULID)"),
            fieldWithPath(prefix + "workspaceId").type(JsonFieldType.STRING)
                    .description("소속 워크스페이스 ID (ULID)"),
            fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
                    .description("프로젝트 이름"),
            fieldWithPath(prefix + "description").type(JsonFieldType.STRING)
                    .description("프로젝트 설명").optional(),
            fieldWithPath(prefix + "myRole").type(JsonFieldType.STRING)
                    .description("현재 사용자의 역할 (OWNER, ADMIN, EDITOR, COMMENTER, VIEWER)"),
            fieldWithPath(prefix + "memberCount").type(JsonFieldType.NUMBER)
                    .description("전체 멤버 수"),
            fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING)
                    .description("생성 시각 (ISO 8601)"),
            fieldWithPath(prefix + "updatedAt").type(JsonFieldType.STRING)
                    .description("수정 시각 (ISO 8601)")
        };
    }

    // ========== POST /api/workspaces/{workspaceId}/projects - 프로젝트 생성 ==========

    /**
     * 프로젝트 생성 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * POST /api/workspaces/{workspaceId}/projects
     * Authorization: Bearer {accessToken}
     *
     * {
     *   "name": "My Project",
     *   "description": "프로젝트 설명",
     *   "settings": {
     *     "theme": "light",
     *     "language": "en",
     *     "defaultView": "table"
     *   }
     * }
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>프로젝트 이름은 필수</li>
     *   <li>생성자가 자동으로 OWNER 역할로 등록됨</li>
     *   <li>settings가 null이면 기본값 사용</li>
     *   <li>워크스페이스에 종속됨</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>워크스페이스의 OWNER 또는 ADMIN만 생성 가능</li>
     * </ul>
     */
    public static Snippet createProjectPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"));
    }

    /**
     * 프로젝트 생성 요청 헤더
     */
    public static Snippet createProjectRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 프로젝트 생성 요청 바디
     */
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

    /**
     * 프로젝트 생성 응답 헤더
     */
    public static Snippet createProjectResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 프로젝트 생성 응답
     */
    public static Snippet createProjectResponse() {
        return createResponseFieldsSnippet(successResponseFields(projectResponseFields()));
    }

    // ========== GET /api/workspaces/{workspaceId}/projects - 프로젝트 목록 조회 ==========

    /**
     * 프로젝트 목록 조회 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * GET /api/workspaces/{workspaceId}/projects?page=0&size=20
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>워크스페이스 내 프로젝트 목록 조회</li>
     *   <li>페이징 지원 (기본값: page=0, size=20)</li>
     *   <li>역할 정보 및 멤버 수 포함</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>워크스페이스 멤버만 조회 가능</li>
     * </ul>
     */
    public static Snippet getProjectsPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"));
    }

    /**
     * 프로젝트 목록 조회 요청 헤더
     */
    public static Snippet getProjectsRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 프로젝트 목록 조회 쿼리 파라미터
     */
    public static Snippet getProjectsQueryParameters() {
        return queryParameters(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20, 최대: 100)").optional());
    }

    /**
     * 프로젝트 목록 조회 응답 헤더
     */
    public static Snippet getProjectsResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 프로젝트 목록 조회 응답
     */
    public static Snippet getProjectsResponse() {
        return createResponseFieldsSnippet(
                successResponseFields(concat(
                        new FieldDescriptor[] {
                            fieldWithPath("result.content[]").type(JsonFieldType.ARRAY)
                                    .description("프로젝트 목록"),
                            fieldWithPath("result.page").type(JsonFieldType.NUMBER)
                                    .description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("result.size").type(JsonFieldType.NUMBER)
                                    .description("페이지 크기"),
                            fieldWithPath("result.totalElements").type(JsonFieldType.NUMBER)
                                    .description("전체 프로젝트 개수"),
                            fieldWithPath("result.totalPages").type(JsonFieldType.NUMBER)
                                    .description("전체 페이지 수")
                        },
                        projectSummaryFields("result.content[].")
                )));
    }

    // ========== GET /api/workspaces/{workspaceId}/projects/{id} - 프로젝트 상세 조회 ==========

    /**
     * 프로젝트 상세 조회 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * GET /api/workspaces/{workspaceId}/projects/{id}
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>프로젝트 상세 정보 조회</li>
     *   <li>설정 정보 포함</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>워크스페이스 멤버 또는 프로젝트 멤버만 조회 가능</li>
     * </ul>
     */
    public static Snippet getProjectPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"),
                parameterWithName("id").description("프로젝트 ID (ULID)"));
    }

    /**
     * 프로젝트 상세 조회 요청 헤더
     */
    public static Snippet getProjectRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 프로젝트 상세 조회 응답 헤더
     */
    public static Snippet getProjectResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 프로젝트 상세 조회 응답
     */
    public static Snippet getProjectResponse() {
        return createResponseFieldsSnippet(successResponseFields(projectResponseFields()));
    }

    // ========== PUT /api/workspaces/{workspaceId}/projects/{id} - 프로젝트 수정 ==========

    /**
     * 프로젝트 수정 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * PUT /api/workspaces/{workspaceId}/projects/{id}
     * Authorization: Bearer {accessToken}
     *
     * {
     *   "name": "Updated Project",
     *   "description": "수정된 설명",
     *   "settings": {
     *     "theme": "dark",
     *     "language": "ko",
     *     "defaultView": "grid"
     *   }
     * }
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>프로젝트 이름은 필수</li>
     *   <li>settings가 null이면 기본값 사용</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>워크스페이스의 OWNER 또는 ADMIN만 수정 가능</li>
     * </ul>
     */
    public static Snippet updateProjectPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"),
                parameterWithName("id").description("프로젝트 ID (ULID)"));
    }

    /**
     * 프로젝트 수정 요청 헤더
     */
    public static Snippet updateProjectRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 프로젝트 수정 요청 바디
     */
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
                        .description("언어 설정").optional(),
                fieldWithPath("settings.defaultView").type(JsonFieldType.STRING)
                        .description("기본 뷰 설정").optional());
    }

    /**
     * 프로젝트 수정 응답 헤더
     */
    public static Snippet updateProjectResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 프로젝트 수정 응답
     */
    public static Snippet updateProjectResponse() {
        return createResponseFieldsSnippet(successResponseFields(projectResponseFields()));
    }

    // ========== DELETE /api/workspaces/{workspaceId}/projects/{id} - 프로젝트 삭제 ==========

    /**
     * 프로젝트 삭제 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * DELETE /api/workspaces/{workspaceId}/projects/{id}
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>프로젝트 삭제 시 하위 리소스도 모두 삭제됨</li>
     *   <li>복구 불가능</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>워크스페이스의 OWNER 또는 ADMIN만 삭제 가능</li>
     * </ul>
     */
    public static Snippet deleteProjectPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"),
                parameterWithName("id").description("프로젝트 ID (ULID)"));
    }

    /**
     * 프로젝트 삭제 요청 헤더
     */
    public static Snippet deleteProjectRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 프로젝트 삭제 응답
     */
    public static Snippet deleteProjectResponse() {
        return createResponseFieldsSnippet(successResponseFieldsWithNullResult());
    }

    // ========== GET /api/workspaces/{workspaceId}/projects/{id}/members - 프로젝트 멤버 조회 ==========

    /**
     * 프로젝트 멤버 조회 경로 파라미터
     *
     * <p>사용 예시:</p>
     * <pre>
     * GET /api/workspaces/{workspaceId}/projects/{id}/members?page=0&size=20
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>프로젝트 멤버 목록 조회</li>
     *   <li>페이징 지원 (기본값: page=0, size=20)</li>
     *   <li>사용자 정보 및 역할 포함</li>
     * </ul>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>JWT 인증 필수</li>
     *   <li>워크스페이스 멤버 또는 프로젝트 멤버만 조회 가능</li>
     * </ul>
     */
    public static Snippet getProjectMembersPathParameters() {
        return pathParameters(
                parameterWithName("workspaceId").description("워크스페이스 ID (ULID)"),
                parameterWithName("id").description("프로젝트 ID (ULID)"));
    }

    /**
     * 프로젝트 멤버 조회 요청 헤더
     */
    public static Snippet getProjectMembersRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    /**
     * 프로젝트 멤버 조회 쿼리 파라미터
     */
    public static Snippet getProjectMembersQueryParameters() {
        return queryParameters(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20, 최대: 100)").optional());
    }

    /**
     * 프로젝트 멤버 조회 응답 헤더
     */
    public static Snippet getProjectMembersResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * 프로젝트 멤버 조회 응답
     */
    public static Snippet getProjectMembersResponse() {
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
                                .description("프로젝트 내 역할 (OWNER, ADMIN, EDITOR, COMMENTER, VIEWER)"),
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