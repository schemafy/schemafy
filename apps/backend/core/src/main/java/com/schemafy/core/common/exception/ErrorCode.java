package com.schemafy.core.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    COMMON_SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001",
            "시스템 오류가 발생했습니다."),
    COMMON_INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "C002",
            "유효하지 않은 파라미터입니다."),
    COMMON_API_VERSION_MISSING(HttpStatus.BAD_REQUEST, "C003",
            "API 버전이 누락되었습니다."),
    COMMON_API_VERSION_INVALID(HttpStatus.BAD_REQUEST, "C004",
            "유효하지 않은 API 버전 형식입니다. (예: v1.0, v2.1)"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C005", "잘못된 입력값입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C006", "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C007",
            "내부 서버 오류가 발생했습니다."),
    ALREADY_DELETED(HttpStatus.CONFLICT, "C008", "이미 삭제된 리소스입니다."),

    // Authentication & Authorization
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "A001",
            "인증이 필요합니다. 유효한 토큰을 제공해주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A003",
            "유효하지 않은 리프레시 토큰입니다."),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰 타입입니다."),
    MISSING_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "리프레시 토큰이 없습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A006", "만료된 토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A007", "유효하지 않은 토큰입니다."),
    INVALID_ACCESS_TOKEN_TYPE(HttpStatus.BAD_REQUEST, "A008", "액세스 토큰이 아닙니다."),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "A009", "위조되거나 손상된 토큰입니다."),
    TOKEN_VALIDATION_ERROR(HttpStatus.UNAUTHORIZED, "A010",
            "토큰 검증 중 오류가 발생했습니다."),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "회원을 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 존재하는 회원입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U003", "로그인에 실패했습니다."),

    // VALIDATION
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "V001", "검증에 실패했습니다."),
    VALIDATION_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "V002",
            "검증 서비스에 연결할 수 없습니다."),
    VALIDATION_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "V003",
            "검증 서비스 요청 시간이 초과되었습니다."),
    VALIDATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "V004",
            "검증 처리 중 오류가 발생했습니다."),

    // ERD
    ERD_SCHEMA_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "스키마를 찾을 수 없습니다."),
    ERD_TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "E002", "테이블을 찾을 수 없습니다."),
    ERD_COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND, "E003", "컬럼을 찾을 수 없습니다."),
    ERD_CONSTRAINT_NOT_FOUND(HttpStatus.NOT_FOUND, "E004", "제약조건을 찾을 수 없습니다."),
    ERD_CONSTRAINT_COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND, "E007",
            "제약조건 컬럼을 찾을 수 없습니다."),
    ERD_INDEX_NOT_FOUND(HttpStatus.NOT_FOUND, "E005", "인덱스를 찾을 수 없습니다."),
    ERD_INDEX_COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND, "E008",
            "인덱스 컬럼을 찾을 수 없습니다."),
    ERD_RELATIONSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "E006", "관계를 찾을 수 없습니다."),
    ERD_RELATIONSHIP_COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND, "E009",
            "관계 컬럼을 찾을 수 없습니다."),
    ERD_VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "E010", "DB 벤더를 찾을 수 없습니다."),

    // WORKSPACE
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "워크스페이스를 찾을 수 없습니다."),
    WORKSPACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "W002",
            "워크스페이스 접근 권한이 없습니다."),
    WORKSPACE_OWNER_ONLY(HttpStatus.FORBIDDEN, "W003",
            "워크스페이스 소유자만 수행할 수 있는 작업입니다."),
    WORKSPACE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "W004",
            "워크스페이스 이름은 필수입니다."),
    WORKSPACE_SETTINGS_INVALID(HttpStatus.BAD_REQUEST, "W005",
            "워크스페이스 설정 형식이 올바르지 않습니다."),
    WORKSPACE_SETTINGS_TOO_LARGE(HttpStatus.BAD_REQUEST, "W006",
            "워크스페이스 설정 크기가 너무 큽니다. (최대 64KB)"),
    WORKSPACE_ALREADY_DELETED(HttpStatus.CONFLICT, "W007",
            "이미 삭제된 워크스페이스입니다."),
    WORKSPACE_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "W008",
            "관리자 권한이 필요합니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "W009", "이미 워크스페이스 멤버입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "W010", "멤버를 찾을 수 없습니다."),
    MEMBER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "W011",
            "워크스페이스 멤버 수 제한(100명)을 초과했습니다."),
    LAST_ADMIN_CANNOT_BE_REMOVED(HttpStatus.BAD_REQUEST, "W012",
            "마지막 관리자는 제거할 수 없습니다."),
    LAST_ADMIN_CANNOT_CHANGE_ROLE(HttpStatus.BAD_REQUEST, "W013",
            "마지막 관리자의 권한은 변경할 수 없습니다."),
    LAST_ADMIN_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "W014",
            "마지막 관리자는 워크스페이스를 떠날 수 없습니다."),

    // PROJECT
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "프로젝트를 찾을 수 없습니다."),
    PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P002", "프로젝트 접근 권한이 없습니다."),
    PROJECT_OWNER_ONLY(HttpStatus.FORBIDDEN, "P003",
            "프로젝트 소유자만 수행할 수 있는 작업입니다."),
    PROJECT_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "P004", "프로젝트 관리자 권한이 필요합니다."),
    PROJECT_WORKSPACE_MISMATCH(HttpStatus.BAD_REQUEST, "P005",
            "프로젝트가 해당 워크스페이스에 속하지 않습니다."),
    PROJECT_SETTINGS_TOO_LARGE(HttpStatus.BAD_REQUEST, "P006",
            "프로젝트 설정 크기가 너무 큽니다. (최대 64KB)"),
    PROJECT_ALREADY_DELETED(HttpStatus.CONFLICT, "P007", "이미 삭제된 프로젝트입니다."),

    // PROJECT MEMBER MANAGEMENT
    CANNOT_CHANGE_OWN_ROLE(HttpStatus.BAD_REQUEST, "PM001",
            "자신의 권한은 변경할 수 없습니다."),
    LAST_OWNER_CANNOT_BE_REMOVED(HttpStatus.BAD_REQUEST, "PM002",
            "마지막 소유자는 제거할 수 없습니다."),
    WORKSPACE_MEMBERSHIP_REQUIRED(HttpStatus.FORBIDDEN, "PM003",
            "프로젝트 접근을 위해서는 워크스페이스 멤버여야 합니다."),
    PROJECT_MEMBER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PM004",
            "프로젝트 멤버 수 제한(100명)을 초과했습니다."),

    // SHARE_LINK
    SHARE_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "공유 링크를 찾을 수 없습니다."),
    SHARE_LINK_EXPIRED(HttpStatus.GONE, "S002", "공유 링크가 만료되었습니다."),
    SHARE_LINK_REVOKED(HttpStatus.FORBIDDEN, "S003", "공유 링크가 비활성화되었습니다."),
    SHARE_LINK_INVALID(HttpStatus.UNAUTHORIZED, "S004", "유효하지 않은 공유 링크입니다."),
    SHARE_LINK_INVALID_PROJECT_ID(HttpStatus.BAD_REQUEST, "S005",
            "프로젝트 ID가 유효하지 않습니다."),
    SHARE_LINK_INVALID_TOKEN_HASH(HttpStatus.BAD_REQUEST, "S006",
            "토큰 해시가 유효하지 않습니다."),
    SHARE_LINK_INVALID_ROLE(HttpStatus.BAD_REQUEST, "S007", "권한이 유효하지 않습니다."),
    SHARE_LINK_INVALID_EXPIRATION(HttpStatus.BAD_REQUEST, "S008",
            "만료 시간은 미래 시간이어야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
