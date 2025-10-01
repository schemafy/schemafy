package com.schemafy.core.common.util;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ULIDUtils {

    /**
     * 새로운 ULID 문자열 생성
     *
     * @return ULID 문자열 (26자)
     */
    public static String generate() {
        return UlidCreator.getUlid().toString();
    }

    /**
     * 새로운 ULID 객체 생성
     *
     * @return Ulid 객체
     */
    public static Ulid generateUlid() {
        return UlidCreator.getUlid();
    }

    /**
     * Monotonic ULID 생성 (동일 밀리초 내 정렬 보장)
     *
     * @return ULID 문자열
     */
    public static String generateMonotonic() {
        return UlidCreator.getMonotonicUlid().toString();
    }

    /**
     * 특정 시간으로 ULID 생성
     *
     * @param timestamp 밀리초 단위 타임스탬프
     * @return ULID 문자열
     */
    public static String generate(long timestamp) {
        return UlidCreator.getUlid(timestamp).toString();
    }

    /**
     * ULID 문자열을 Ulid 객체로 변환
     *
     * @param ulidString ULID 문자열
     * @return Ulid 객체
     */
    public static Ulid parse(String ulidString) {
        return Ulid.from(ulidString);
    }

    /**
     * ULID에서 타임스탬프 추출
     *
     * @param ulidString ULID 문자열
     * @return 밀리초 단위 타임스탬프
     */
    public static long getTimestamp(String ulidString) {
        return Ulid.from(ulidString).getTime();
    }

    /**
     * ULID에서 Instant 추출
     *
     * @param ulidString ULID 문자열
     * @return Instant 객체
     */
    public static Instant getInstant(String ulidString) {
        return Ulid.from(ulidString).getInstant();
    }

    /**
     * ULID 유효성 검증
     *
     * @param ulidString 검증할 문자열
     * @return 유효하면 true
     */
    public static boolean isValid(String ulidString) {
        if (ulidString == null || ulidString.length() != 26) {
            return false;
        }

        try {
            Ulid.from(ulidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * ULID를 UUID 형식의 문자열로 변환
     *
     * @param ulidString ULID 문자열
     * @return UUID 형식 문자열
     */
    public static String toUuid(String ulidString) {
        return Ulid.from(ulidString).toUuid().toString();
    }

    /**
     * ULID를 바이트 배열로 변환
     *
     * @param ulidString ULID 문자열
     * @return 16바이트 배열
     */
    public static byte[] toBytes(String ulidString) {
        return Ulid.from(ulidString).toBytes();
    }
}
