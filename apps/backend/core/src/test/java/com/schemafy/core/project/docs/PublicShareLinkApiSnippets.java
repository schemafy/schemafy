package com.schemafy.core.project.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

/** Public ShareLink API 문서화를 위한 스니펫 제공 클래스 */
public class PublicShareLinkApiSnippets extends RestDocsSnippets {

  // ========== ProjectSettings 공통 필드 ==========

  /** ProjectSettings 응답 필드 */
  private static FieldDescriptor[] projectSettingsFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "theme").type(JsonFieldType.STRING)
          .description("테마 설정 (예: light, dark)"),
      fieldWithPath(prefix + "language").type(JsonFieldType.STRING)
          .description("언어 설정 (예: ko, en)")
    };
  }

  // ========== GET /public/api/v1.0/share/{code} - 공유 링크로 프로젝트 접근 ==========

  /** 공유 링크 접근 경로 파라미터 */
  public static Snippet accessByCodePathParameters() {
    return pathParameters(
        parameterWithName("code").description("공유 링크 코드"));
  }

  /** 공유 링크 접근 응답 헤더 */
  public static Snippet accessByCodeResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 공유 링크 접근 응답 */
  public static Snippet accessByCodeResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            concat(
                new FieldDescriptor[] {
                  fieldWithPath("result.projectId")
                      .type(JsonFieldType.STRING)
                      .description("프로젝트 ID"),
                  fieldWithPath("result.projectName")
                      .type(JsonFieldType.STRING)
                      .description("프로젝트 이름"),
                  fieldWithPath("result.description")
                      .type(JsonFieldType.STRING)
                      .description("프로젝트 설명").optional(),
                  fieldWithPath("result.settings")
                      .type(JsonFieldType.OBJECT)
                      .description("프로젝트 설정")
                },
                projectSettingsFields("result.settings."))));
  }

}
