package com.schemafy.core.common.resolver;

import com.schemafy.core.common.annotation.ApiVersion;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.regex.Pattern;

public class ApiVersionArgumentResolver implements HandlerMethodArgumentResolver {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+\\.\\d+$");

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ApiVersion.class);
    }

    @Override
    @NonNull
    public Mono<Object> resolveArgument(
            @NonNull MethodParameter parameter,
            @NonNull BindingContext bindingContext,
            @NonNull ServerWebExchange exchange) {

        Map<String, String> pathVariables = exchange.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (pathVariables == null || !pathVariables.containsKey("version")) {
            return Mono.error(new IllegalStateException("Path variable 'version' not found"));
        }

        String versionString = pathVariables.get("version");

        if (versionString == null || !VERSION_PATTERN.matcher(versionString).matches()) {
            return Mono.error(new BusinessException(ErrorCode.COMMON_API_VERSION_INVALID));
        }

        if (parameter.getParameterType().equals(String.class)) {
            return Mono.just(versionString);
        }

        return Mono.error(new IllegalArgumentException(
                "@ApiVersion can only be used with String parameter type"));
    }
}
