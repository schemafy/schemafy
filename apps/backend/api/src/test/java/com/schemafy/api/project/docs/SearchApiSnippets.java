package com.schemafy.api.project.docs;

import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.api.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

/** Search API 문서화를 위한 스니펫 제공 클래스 */
public class SearchApiSnippets extends RestDocsSnippets {

  public static Snippet searchRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  public static Snippet searchResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet projectQueryParameters() {
    return queryParameters(
        parameterWithName("category")
            .description("프로젝트 검색 범위 (WORKSPACE, SHARED)"),
        parameterWithName("workspaceId")
            .description("WORKSPACE 검색 대상 워크스페이스 ID")
            .optional(),
        parameterWithName("search")
            .description("프로젝트 이름 검색어 (1-100자)"),
        parameterWithName("page")
            .description("페이지 번호 (0부터 시작, 기본값: 0, 최대: 10000)")
            .optional(),
        parameterWithName("size")
            .description("페이지 크기 (기본값: 5, 범위: 1-100)")
            .optional());
  }

  public static Snippet memberQueryParameters() {
    return queryParameters(
        parameterWithName("category")
            .description("멤버 검색 범위 (WORKSPACE, PROJECT)"),
        parameterWithName("workspaceId")
            .description("WORKSPACE 검색 대상 워크스페이스 ID")
            .optional(),
        parameterWithName("projectId")
            .description("PROJECT 검색 대상 프로젝트 ID")
            .optional(),
        parameterWithName("search")
            .description("사용자 이름 또는 이메일 검색어 (1-100자)"),
        parameterWithName("page")
            .description("페이지 번호 (0부터 시작, 기본값: 0, 최대: 10000)")
            .optional(),
        parameterWithName("size")
            .description("페이지 크기 (기본값: 5, 범위: 1-100)")
            .optional());
  }

  public static Snippet projectResponse() {
    return createResponseFieldsSnippet(
        fieldWithPath("content[]").type(JsonFieldType.ARRAY)
            .description("검색된 프로젝트 목록"),
        fieldWithPath("content[].id").type(JsonFieldType.STRING)
            .description("프로젝트 ID"),
        fieldWithPath("content[].workspaceId").type(JsonFieldType.STRING)
            .description("워크스페이스 ID"),
        fieldWithPath("content[].dbVendorId").type(JsonFieldType.NUMBER)
            .description("데이터베이스 벤더 ID"),
        fieldWithPath("content[].name").type(JsonFieldType.STRING)
            .description("프로젝트 이름"),
        fieldWithPath("content[].description").type(JsonFieldType.STRING)
            .description("프로젝트 설명").optional(),
        fieldWithPath("content[].myRole").type(JsonFieldType.STRING)
            .description("요청자의 프로젝트 역할"),
        fieldWithPath("content[].createdAt").type(JsonFieldType.STRING)
            .description("프로젝트 생성 시각"),
        fieldWithPath("content[].updatedAt").type(JsonFieldType.STRING)
            .description("프로젝트 수정 시각"),
        fieldWithPath("page").type(JsonFieldType.NUMBER)
            .description("현재 페이지 번호"),
        fieldWithPath("size").type(JsonFieldType.NUMBER)
            .description("페이지 크기"),
        fieldWithPath("totalElements").type(JsonFieldType.NUMBER)
            .description("전체 검색 결과 수"),
        fieldWithPath("totalPages").type(JsonFieldType.NUMBER)
            .description("전체 페이지 수"));
  }

  public static Snippet memberResponse() {
    return createResponseFieldsSnippet(
        fieldWithPath("content[]").type(JsonFieldType.ARRAY)
            .description("검색된 멤버 목록"),
        fieldWithPath("content[].userId").type(JsonFieldType.STRING)
            .description("사용자 ID"),
        fieldWithPath("content[].userName").type(JsonFieldType.STRING)
            .description("사용자 이름"),
        fieldWithPath("content[].userEmail").type(JsonFieldType.STRING)
            .description("사용자 이메일"),
        fieldWithPath("content[].role").type(JsonFieldType.STRING)
            .description("검색 범위 내 사용자 역할"),
        fieldWithPath("content[].joinedAt").type(JsonFieldType.STRING)
            .description("멤버 가입 시각"),
        fieldWithPath("page").type(JsonFieldType.NUMBER)
            .description("현재 페이지 번호"),
        fieldWithPath("size").type(JsonFieldType.NUMBER)
            .description("페이지 크기"),
        fieldWithPath("totalElements").type(JsonFieldType.NUMBER)
            .description("전체 검색 결과 수"),
        fieldWithPath("totalPages").type(JsonFieldType.NUMBER)
            .description("전체 페이지 수"));
  }

}
