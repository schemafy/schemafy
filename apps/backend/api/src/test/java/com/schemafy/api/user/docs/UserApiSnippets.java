package com.schemafy.api.user.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.api.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

/** User API 문서화를 위한 스니펫 제공 클래스 - User 도메인에 특화된 스니펫 정의 */
public class UserApiSnippets extends RestDocsSnippets {

  // ========== User 도메인 공통 필드 ==========

  /** 사용자 정보 응답 필드 (result.* 형태) */
  private static FieldDescriptor[] userInfoFields() {
    return new FieldDescriptor[] {
      fieldWithPath("id").type(JsonFieldType.STRING)
          .description("사용자 고유 ID (ULID)"),
      fieldWithPath("email").type(JsonFieldType.STRING)
          .description("사용자 이메일"),
      fieldWithPath("name").type(JsonFieldType.STRING)
          .description("사용자 이름")
    };
  }

  // ========== 회원가입 API ==========

  public static Snippet sendSignUpEmailCodeRequest() {
    return requestFields(
        fieldWithPath("email").type(JsonFieldType.STRING)
            .description("인증 코드를 발송할 이메일 (형식: example@domain.com)"));
  }

  public static Snippet signUpRequest() {
    return requestFields(
        fieldWithPath("email").type(JsonFieldType.STRING)
            .description("인증 완료된 사용자 이메일 (형식: example@domain.com)"),
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("사용자 이름 (최대 200자)"),
        fieldWithPath("password").type(JsonFieldType.STRING)
            .description("비밀번호 (8자 이상)"),
        fieldWithPath("signupVerificationToken").type(JsonFieldType.STRING)
            .description("이메일 코드 인증 성공 시 발급된 회원가입 검증 토큰 (16~128자)"));
  }

  public static Snippet signUpResponseHeaders() {
    return createResponseHeadersSnippet(authResponseHeaders());
  }

  public static Snippet signUpResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            fieldWithPath("email").type(JsonFieldType.STRING)
                .description("인증 코드를 발송한 이메일"),
            fieldWithPath("expiresAt").type(JsonFieldType.STRING)
                .description("인증 코드 만료 시각")));
  }

  public static Snippet verifySignUpEmailRequest() {
    return requestFields(
        fieldWithPath("email").type(JsonFieldType.STRING)
            .description("회원가입 이메일"),
        fieldWithPath("code").type(JsonFieldType.STRING)
            .description("SMTP로 발송된 6자리 인증 코드"));
  }

  public static Snippet verifySignUpEmailResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(
            fieldWithPath("email").type(JsonFieldType.STRING)
                .description("인증이 완료된 이메일"),
            fieldWithPath("signupVerificationToken").type(JsonFieldType.STRING)
                .description("최종 회원가입에 사용할 검증 토큰"),
            fieldWithPath("expiresAt").type(JsonFieldType.STRING)
                .description("회원가입 검증 토큰 만료 시각")));
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

  public static Snippet getUserRequestHeaders() { return createRequestHeadersSnippet(authorizationHeader()); }

  public static Snippet getUserResponseHeaders() { return createResponseHeadersSnippet(commonResponseHeaders()); }

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
    return createResponseHeadersSnippet(
        headerWithName("Authorization").description("발급된 JWT 액세스 토큰"),
        headerWithName("Set-Cookie")
            .description("발급된 리프레시 토큰 (HttpOnly 쿠키)"));
  }

  public static Snippet refreshTokenResponse() {
    return createResponseFieldsSnippet(
        successResponseFieldsWithNullResult());
  }

  // ========== 로그아웃 API ==========

  public static Snippet logoutRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  public static Snippet logoutResponseHeaders() {
    return createResponseHeadersSnippet(
        headerWithName("Set-Cookie")
            .description("만료된 accessToken 및 refreshToken 쿠키 (Max-Age=0)"));
  }

  public static Snippet logoutResponse() {
    return createResponseFieldsSnippet(
        successResponseFieldsWithNullResult());
  }

  // ========== GitHub OAuth API ==========

  public static Snippet oauthAuthorizeResponseHeaders() {
    return createResponseHeadersSnippet(
        headerWithName("Location")
            .description("GitHub OAuth 인가 페이지 URL"),
        headerWithName("Set-Cookie")
            .description("CSRF 방어용 state 쿠키 (oauth_state, HttpOnly, SameSite=Lax, Max-Age=600)"));
  }

}
