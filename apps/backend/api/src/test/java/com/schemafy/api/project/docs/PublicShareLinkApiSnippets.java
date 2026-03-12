package com.schemafy.api.project.docs;

import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.api.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

/** Public ShareLink API 문서화를 위한 스니펫 제공 클래스 */
public class PublicShareLinkApiSnippets extends RestDocsSnippets {

  // ========== GET /public/api/v1.0/share/{code} - 공유 링크로 프로젝트 접근 ==========

  /** 공유 링크 접근 경로 파라미터 */
  public static Snippet accessByLinkPathParameters() {
    return pathParameters(
        parameterWithName("code").description("공유 링크 코드"));
  }

  /** 공유 링크 접근 응답 헤더 */
  public static Snippet accessByLinkResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 공유 링크 접근 응답 */
  public static Snippet accessByLinkResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            fieldWithPath("projectId")
                .type(JsonFieldType.STRING)
                .description("프로젝트 ID"),
            fieldWithPath("projectName")
                .type(JsonFieldType.STRING)
                .description("프로젝트 이름"),
            fieldWithPath("description")
                .type(JsonFieldType.STRING)
                .description("프로젝트 설명").optional()));
  }

}
