package com.schemafy.core.common.type;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    private final boolean success;
    private final T result;
    private final ErrorResponse error;

    public static <T> BaseResponse<T> success(T result) {
        return new BaseResponse<>(true, result, null);
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(false, null,
                new ErrorResponse(code, message));
    }

    public record ErrorResponse(String code, String message) {
    }
}
