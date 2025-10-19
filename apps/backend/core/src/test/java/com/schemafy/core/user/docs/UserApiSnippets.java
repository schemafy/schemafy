package com.schemafy.core.user.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

/**
 * User API 문서화를 위한 스니펫 제공 클래스 - User 도메인에 특화된 스니펫 정의
 */
public class UserApiSnippets extends RestDocsSnippets {

    // ========== User 도메인 공통 필드 ==========

    /**
     * 사용자 정보 응답 필드 (result.* 형태)
     */
    private static FieldDescriptor[] userInfoFields() {
        return new FieldDescriptor[] {
            fieldWithPath("result.id").type(JsonFieldType.STRING)
                    .description("사용자 고유 ID (ULID)"),
            fieldWithPath("result.email").type(JsonFieldType.STRING)
                    .description("사용자 이메일"),
            fieldWithPath("result.name").type(JsonFieldType.STRING)
                    .description("사용자 이름")
        };
    }

    // ========== 회원가입 API ==========

    public static Snippet signUpRequest() {
        return requestFields(
                fieldWithPath("email").type(JsonFieldType.STRING)
                        .description("사용자 이메일 (형식: example@domain.com)"),
                fieldWithPath("name").type(JsonFieldType.STRING)
                        .description("사용자 이름"),
                fieldWithPath("password").type(JsonFieldType.STRING)
                        .description("비밀번호"));
    }

    public static Snippet signUpResponseHeaders() {
        return createResponseHeadersSnippet(authResponseHeaders());
    }

    public static Snippet signUpResponse() {
        return createResponseFieldsSnippet(
                successResponseFields(userInfoFields()));
    }

    // ========== 로그인 API ==========

    public static Snippet loginRequest() {
        return requestFields(
                fieldWithPath("email").type(JsonFieldType.STRING)
                        .description("사용자 이메일"),
                fieldWithPath("password").type(JsonFieldType.STRING)
                        .description("비밀번호"));
    }

    public static Snippet loginResponseHeaders() {
        return createResponseHeadersSnippet(authResponseHeaders());
    }

    public static Snippet loginResponse() {
        return createResponseFieldsSnippet(
                successResponseFields(userInfoFields()));
    }

    // ========== 사용자 조회 API ==========

    public static Snippet getUserPathParameters() {
        return pathParameters(
                parameterWithName("userId").description("조회할 사용자 ID (ULID)"));
    }

    public static Snippet getUserRequestHeaders() {
        return createRequestHeadersSnippet(authorizationHeader());
    }

    public static Snippet getUserResponseHeaders() {
        return createResponseHeadersSnippet(commonResponseHeaders());
    }

    public static Snippet getUserResponse() {
        return createResponseFieldsSnippet(
                successResponseFields(userInfoFields()));
    }

    // ========== 토큰 갱신 API ==========

    public static Snippet refreshTokenRequestCookies() {
        return requestCookies(
                cookieWithName("refreshToken").description("리프레시 토큰 (JWT)"));
    }

    public static Snippet refreshTokenResponseHeaders() {
        return createResponseHeadersSnippet(authResponseHeaders());
    }

    public static Snippet refreshTokenResponse() {
        return createResponseFieldsSnippet(
                successResponseFieldsWithNullResult());
    }
}
