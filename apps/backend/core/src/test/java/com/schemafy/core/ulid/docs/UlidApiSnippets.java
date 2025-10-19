package com.schemafy.core.ulid.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

/**
 * ULID API 문서화를 위한 스니펫 제공 클래스
 */
public class UlidApiSnippets extends RestDocsSnippets {

    // ========== ULID 도메인 공통 필드 ==========

    /**
     * ULID 응답 필드 (result.* 형태)
     */
    private static FieldDescriptor[] ulidResponseFields() {
        return new FieldDescriptor[] {
                fieldWithPath("result.ulid").type(JsonFieldType.STRING)
                        .description("생성된 ULID 문자열")
        };
    }

    // ========== ULID 생성 API ==========

    /**
     * ULID 생성 요청 헤더
     */
    public static Snippet generateUlidRequestHeaders() {
        return createRequestHeadersSnippet(
                headerWithName("Accept").description("요청 응답 포맷(Accept 헤더)")
        );
    }

    /**
     * ULID 생성 응답 헤더
     */
    public static Snippet generateUlidResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    /**
     * ULID 생성 응답 필드
     */
    public static Snippet generateUlidResponse() {
        return createResponseFieldsSnippet(successResponseFields(ulidResponseFields()));
    }
}