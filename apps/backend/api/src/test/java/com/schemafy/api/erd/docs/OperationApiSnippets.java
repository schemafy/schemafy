package com.schemafy.api.erd.docs;

import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.api.collaboration.constant.CollaborationConstants;
import com.schemafy.api.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

public class OperationApiSnippets extends RestDocsSnippets {

  private static HeaderDescriptor[] operationRequestHeaders() {
    return new HeaderDescriptor[] {
      headerWithName("Accept").description("응답 포맷").optional(),
      headerWithName(CollaborationConstants.SESSION_ID_HEADER)
          .description("자기 세션 브로드캐스트 제외용 WebSocket 세션 ID").optional(),
      headerWithName(CollaborationConstants.CLIENT_OPERATION_ID_HEADER)
          .description("클라이언트가 생성한 operation 상관관계 ID").optional(),
      headerWithName(CollaborationConstants.BASE_SCHEMA_REVISION_HEADER)
          .description("클라이언트가 알고 있던 schema revision").optional()
    };
  }

  public static Snippet undoPathParameters() {
    return pathParameters(
        parameterWithName("opId")
            .description("undo 대상 operation ID"));
  }

  public static Snippet undoRequestHeaders() {
    return createRequestHeadersSnippet(operationRequestHeaders());
  }

  public static Snippet undoResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet undoResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  public static Snippet redoPathParameters() {
    return pathParameters(
        parameterWithName("opId")
            .description("redo 대상 operation ID"));
  }

  public static Snippet redoRequestHeaders() {
    return createRequestHeadersSnippet(operationRequestHeaders());
  }

  public static Snippet redoResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet redoResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

}
