package com.schemafy.core.erd.service.util.mysql;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.core.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.*;

class MySqlDdlUtilsTest {

  @Test
  void escapeIdentifier_withNull_returnsEmptyString() {
    assertEquals("", MySqlDdlUtils.escapeIdentifier(null));
  }

  @Test
  void escapeIdentifier_withBacktick_escapesCorrectly() {
    assertEquals("table``name", MySqlDdlUtils.escapeIdentifier("table`name"));
  }

  @Test
  void escapeIdentifier_withNormalString_returnsAsIs() {
    assertEquals("users", MySqlDdlUtils.escapeIdentifier("users"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { "   ", "\t" })
  void requireNonBlank_withBlankValues_throwsException(String value) {
    assertThrows(BusinessException.class,
        () -> MySqlDdlUtils.requireNonBlank(value, "field"));
  }

  @Test
  void requireNonBlank_withValidValue_doesNotThrow() {
    assertDoesNotThrow(() -> MySqlDdlUtils.requireNonBlank("value", "field"));
  }

  @Test
  void escapeString_withNull_returnsEmptyString() {
    assertEquals("", MySqlDdlUtils.escapeString(null));
  }

  @Test
  void escapeString_escapesBackslashAndQuote() {
    assertEquals("test\\\\value''s", MySqlDdlUtils.escapeString("test\\value's"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "VARCHAR", "INT", "BIGINT", "TEXT", "DECIMAL" })
  void sanitizeDataType_withValidTypes_returnsNormalized(String type) {
    assertEquals(type, MySqlDdlUtils.sanitizeDataType(type.toLowerCase()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void sanitizeDataType_withNullOrEmpty_throwsException(String type) {
    assertThrows(BusinessException.class,
        () -> MySqlDdlUtils.sanitizeDataType(type));
  }

  @Test
  void sanitizeDataType_withInvalidFormat_throwsException() {
    assertThrows(BusinessException.class,
        () -> MySqlDdlUtils.sanitizeDataType("123invalid"));
  }

  @ParameterizedTest
  @CsvSource({ "10,10", "'10,2','10,2'" })
  void sanitizeLengthScale_withValidFormats_returnsOptional(String input, String expected) {
    assertEquals(Optional.of(expected), MySqlDdlUtils.sanitizeLengthScale(input));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void sanitizeLengthScale_withNullOrEmpty_returnsEmpty(String input) {
    assertEquals(Optional.empty(), MySqlDdlUtils.sanitizeLengthScale(input));
  }

  @Test
  void sanitizeLengthScale_withInvalidFormat_throwsException() {
    assertThrows(BusinessException.class,
        () -> MySqlDdlUtils.sanitizeLengthScale("abc"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "utf8", "utf8mb4", "latin1" })
  void sanitizeCharset_withValidCharsets_returnsOptional(String charset) {
    assertEquals(Optional.of(charset), MySqlDdlUtils.sanitizeCharset(charset));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void sanitizeCharset_withNullOrEmpty_returnsEmpty(String charset) {
    assertEquals(Optional.empty(), MySqlDdlUtils.sanitizeCharset(charset));
  }

  @ParameterizedTest
  @ValueSource(strings = { "BTREE", "HASH", "FULLTEXT", "SPATIAL" })
  void sanitizeIndexType_withValidTypes_returnsOptional(String type) {
    assertEquals(Optional.of(type), MySqlDdlUtils.sanitizeIndexType(type.toLowerCase()));
  }

  @Test
  void sanitizeIndexType_withInvalidType_throwsException() {
    assertThrows(BusinessException.class,
        () -> MySqlDdlUtils.sanitizeIndexType("INVALID"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "ASC", "DESC" })
  void sanitizeSortDirection_withValidDirections_returnsOptional(String dir) {
    assertEquals(Optional.of(dir), MySqlDdlUtils.sanitizeSortDirection(dir.toLowerCase()));
  }

  @Test
  void sanitizeSortDirection_withInvalidDirection_throwsException() {
    assertThrows(BusinessException.class,
        () -> MySqlDdlUtils.sanitizeSortDirection("INVALID"));
  }

  @Test
  void quoteColumn_withValidColumnId_returnsQuotedName() {
    Map<String, String> columnMap = Map.of("col1", "user_name");
    assertEquals("`user_name`", MySqlDdlUtils.quoteColumn(columnMap, "col1"));
  }

  @Test
  void quoteColumn_withColumnNotFound_throwsException() {
    Map<String, String> columnMap = Map.of("col1", "user_name");
    assertThrows(BusinessException.class,
        () -> MySqlDdlUtils.quoteColumn(columnMap, "col2"));
  }

  @Test
  void quoteColumn_escapesBackticksInColumnName() {
    Map<String, String> columnMap = Map.of("col1", "user`name");
    assertEquals("`user``name`", MySqlDdlUtils.quoteColumn(columnMap, "col1"));
  }

}
