package com.schemafy.api.project.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.api.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

public class MyInvitationApiSnippets extends RestDocsSnippets {

  private static FieldDescriptor[] myInvitationResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("초대 고유 ID"),
      fieldWithPath(prefix + "type").type(JsonFieldType.STRING)
          .description("초대 대상 타입 (WORKSPACE/PROJECT)"),
      fieldWithPath(prefix + "targetId").type(JsonFieldType.STRING)
          .description("초대 대상 ID. WORKSPACE면 workspaceId, PROJECT면 projectId"),
      fieldWithPath(prefix + "targetName").type(JsonFieldType.STRING)
          .description("초대 대상 이름"),
      fieldWithPath(prefix + "targetDescription").type(JsonFieldType.VARIES)
          .description("초대 대상 설명 (nullable)"),
      fieldWithPath(prefix + "invitedEmail").type(JsonFieldType.STRING)
          .description("초대된 사용자 이메일"),
      fieldWithPath(prefix + "invitedRole").type(JsonFieldType.STRING)
          .description("초대된 역할"),
      fieldWithPath(prefix + "invitedBy").type(JsonFieldType.STRING)
          .description("초대한 사용자 ID"),
      fieldWithPath(prefix + "status").type(JsonFieldType.STRING)
          .description("초대 상태 (PENDING만 반환)"),
      fieldWithPath(prefix + "expiresAt").type(JsonFieldType.STRING)
          .description("초대 만료 시각"),
      fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING)
          .description("초대 생성 시각")
    };
  }

  public static Snippet listMyInvitationsRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  public static Snippet listMyInvitationsQueryParameters() {
    return queryParameters(
        parameterWithName("cursorId")
            .description("다음 페이지 조회용 ULID cursor").optional(),
        parameterWithName("size")
            .description("조회 개수 (기본값: 5, 범위: 1-100)").optional());
  }

  public static Snippet listMyInvitationsResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet listMyInvitationsResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(concat(
            new FieldDescriptor[] {
              fieldWithPath("content[]")
                  .type(JsonFieldType.ARRAY)
                  .description("통합 초대 목록"),
              fieldWithPath("size")
                  .type(JsonFieldType.NUMBER)
                  .description("실제 적용된 페이지 크기"),
              fieldWithPath("hasNext")
                  .type(JsonFieldType.BOOLEAN)
                  .description("다음 페이지 존재 여부"),
              fieldWithPath("nextCursorId")
                  .type(JsonFieldType.VARIES)
                  .description("다음 페이지 조회용 cursor ID. 마지막 페이지면 null")
                  .optional()
            },
            myInvitationResponseFields("content[]."))));
  }

}
