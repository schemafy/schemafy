package com.schemafy.api.common.constant;

public final class ApiPath {

  private ApiPath() {}

  public static final String PUBLIC_API = "/public/api/{version}";
  public static final String API = "/api/{version}";
  private static final String[] OPEN_API_DOCS_PATH_PATTERNS = {
    "/v3/api-docs/**",
    "/openapi/**",
    "/swagger-ui.html",
    "/swagger-ui-hmac.html",
    "/swagger-ui-hmac/**",
    "/swagger-ui/**",
    "/webjars/swagger-ui/**" };

  public static String[] openApiDocsPathPatterns() {
    return OPEN_API_DOCS_PATH_PATTERNS.clone();
  }

  public static boolean isOpenApiDocsPath(String path) {
    return path.startsWith("/v3/api-docs")
        || path.startsWith("/openapi/")
        || path.equals("/swagger-ui.html")
        || path.equals("/swagger-ui-hmac.html")
        || path.startsWith("/swagger-ui-hmac/")
        || path.startsWith("/swagger-ui/")
        || path.startsWith("/webjars/swagger-ui/");
  }

}
