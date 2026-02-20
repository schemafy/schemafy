package com.schemafy.core.common.docs;

import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

/** REST Docs 문서화를 위한 공통 스니펫 제공 추상 클래스 - 도메인별 ApiSnippets 클래스는 이 클래스를 상속받아 구현 */
public abstract class RestDocsSnippets {

  // ========== 헤더 메서드 (하위 클래스에서 사용) ==========

  /** 공통 요청 헤더 생성 */
  protected static HeaderDescriptor[] commonRequestHeaders() {
    return new HeaderDescriptor[] {
      headerWithName("Content-Type").description("요청 컨텐츠 타입").optional(),
      headerWithName("Accept").description("응답 포맷").optional()
    };
  }

  /** Authorization 헤더 생성 (JWT 인증용) */
  protected static HeaderDescriptor authorizationHeader() {
    return headerWithName("Authorization")
        .description("JWT 액세스 토큰 (Bearer {token})");
  }

  /** 공통 응답 헤더 생성 */
  protected static HeaderDescriptor[] commonResponseHeaders() {
    return new HeaderDescriptor[] {
      headerWithName("Content-Type").description("응답 컨텐츠 타입")
    };
  }

  /** 인증 응답 헤더 생성 (JWT 토큰 발급 시) */
  protected static HeaderDescriptor[] authResponseHeaders() {
    return new HeaderDescriptor[] {
      headerWithName("Content-Type").description("응답 컨텐츠 타입"),
      headerWithName("Authorization").description("발급된 JWT 액세스 토큰"),
      headerWithName("Set-Cookie")
          .description("발급된 리프레시 토큰 (HttpOnly 쿠키)")
    };
  }

  // ========== 응답 필드 메서드 (하위 클래스에서 사용) ==========

  /** 성공 응답 필드 생성 (result 필드 포함)
   *
   * @param resultFields result 객체 내부 필드들 */
  protected static FieldDescriptor[] successResponseFields(
      FieldDescriptor... resultFields) {
    FieldDescriptor[] baseFields = new FieldDescriptor[] {
      fieldWithPath("success").type(JsonFieldType.BOOLEAN)
          .description("요청 성공 여부"),
      fieldWithPath("result").type(JsonFieldType.OBJECT)
          .description("응답 데이터").optional()
    };

    return concat(baseFields, resultFields);
  }

  /** 성공 응답 필드 생성 (result가 null인 경우) */
  protected static FieldDescriptor[] successResponseFieldsWithNullResult() {
    return new FieldDescriptor[] {
      fieldWithPath("success").type(JsonFieldType.BOOLEAN)
          .description("요청 성공 여부")
    };
  }

  /** MutationResponse 응답 필드 생성 (data 필드 포함)
   *
   * @param dataFields result.data 객체 내부 필드들 */
  protected static FieldDescriptor[] mutationResponseFields(
      FieldDescriptor... dataFields) {
    FieldDescriptor[] baseFields = new FieldDescriptor[] {
      fieldWithPath("success").type(JsonFieldType.BOOLEAN)
          .description("요청 성공 여부"),
      fieldWithPath("result").type(JsonFieldType.OBJECT)
          .description("뮤테이션 결과"),
      fieldWithPath("result.data").type(JsonFieldType.OBJECT)
          .description("생성/수정된 데이터").optional(),
      fieldWithPath("result.affectedTableIds").type(JsonFieldType.ARRAY)
          .description("영향받은 테이블 ID 목록"),
      fieldWithPath("error").type(JsonFieldType.NULL)
          .description("에러 정보 (성공 시 null)").optional()
    };
    return concat(baseFields, dataFields);
  }

  /** MutationResponse 응답 필드 생성 (data가 null인 경우) */
  protected static FieldDescriptor[] mutationResponseFieldsWithNullData() {
    return new FieldDescriptor[] {
      fieldWithPath("success").type(JsonFieldType.BOOLEAN)
          .description("요청 성공 여부"),
      fieldWithPath("result").type(JsonFieldType.OBJECT)
          .description("뮤테이션 결과"),
      fieldWithPath("result.data").type(JsonFieldType.NULL)
          .description("응답 데이터 (없음)").optional(),
      fieldWithPath("result.affectedTableIds").type(JsonFieldType.ARRAY)
          .description("영향받은 테이블 ID 목록"),
      fieldWithPath("error").type(JsonFieldType.NULL)
          .description("에러 정보 (성공 시 null)").optional()
    };
  }

  /** 에러 응답 필드 생성 */
  protected static FieldDescriptor[] errorResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("success").type(JsonFieldType.BOOLEAN)
          .description("요청 성공 여부 (항상 false)"),
      fieldWithPath("result").type(JsonFieldType.NULL)
          .description("응답 데이터 (에러 시 null)").optional(),
      fieldWithPath("error").type(JsonFieldType.OBJECT)
          .description("에러 정보"),
      fieldWithPath("error.code").type(JsonFieldType.STRING)
          .description("에러 코드"),
      fieldWithPath("error.message").type(JsonFieldType.STRING)
          .description("에러 메시지")
    };
  }

  // ========== Snippet 생성 헬퍼 메서드 ==========

  /** 요청 헤더 Snippet 생성 */
  protected static Snippet createRequestHeadersSnippet(
      HeaderDescriptor... headers) {
    return requestHeaders(headers);
  }

  /** 응답 헤더 Snippet 생성 */
  protected static Snippet createResponseHeadersSnippet(
      HeaderDescriptor... headers) {
    return responseHeaders(headers);
  }

  /** 응답 필드 Snippet 생성 */
  protected static Snippet createResponseFieldsSnippet(
      FieldDescriptor... fields) {
    return responseFields(fields);
  }

  // ========== 유틸리티 메서드 ==========

  /** 배열 병합 유틸리티 */
  protected static FieldDescriptor[] concat(FieldDescriptor[] first,
      FieldDescriptor[] second) {
    FieldDescriptor[] result = new FieldDescriptor[first.length
        + second.length];
    System.arraycopy(first, 0, result, 0, first.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

}
