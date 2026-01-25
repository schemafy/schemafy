package com.schemafy.domain.erd.schema.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Schema 도메인 테스트")
class SchemaTest {

  private static final String ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
  private static final String NAME = "test_schema";
  private static final String CHARSET = "utf8mb4";
  private static final String COLLATION = "utf8mb4_general_ci";

  @Nested
  @DisplayName("정상 생성 테스트")
  class CreateSchemaTests {

    @Test
    @DisplayName("모든 필드로 생성")
    void createSchema_WithAllFields() {
      Schema schema = new Schema(ID, PROJECT_ID, "mysql", NAME, CHARSET, COLLATION);

      assertEquals(ID, schema.id());
      assertEquals(PROJECT_ID, schema.projectId());
      assertEquals("mysql", schema.dbVendorName());
      assertEquals(NAME, schema.name());
      assertEquals(CHARSET, schema.charset());
      assertEquals(COLLATION, schema.collation());
    }

    @Test
    @DisplayName("charset과 collation이 null일 때 기본값 적용")
    void createSchema_WithDefaultCharsetAndCollation() {
      Schema schema = new Schema(ID, PROJECT_ID, "mysql", NAME, null, null);

      assertEquals("utf8mb4", schema.charset());
      assertEquals("utf8mb4_general_ci", schema.collation());
    }

  }

  @Nested
  @DisplayName("기본값 설정 테스트")
  class DefaultValueTests {

    @Test
    @DisplayName("MySQL일 때 기본 charset은 utf8mb4")
    void defaultCharset_ForMysql() {
      Schema schema = new Schema(ID, PROJECT_ID, "mysql", NAME, null, null);

      assertEquals("utf8mb4", schema.charset());
    }

    @Test
    @DisplayName("MariaDB일 때 기본 charset은 utf8mb4")
    void defaultCharset_ForMariadb() {
      Schema schema = new Schema(ID, PROJECT_ID, "mariadb", NAME, null, null);

      assertEquals("utf8mb4", schema.charset());
    }

    @Test
    @DisplayName("기타 벤더일 때 기본 charset은 utf8")
    void defaultCharset_ForOtherVendor() {
      Schema schema = new Schema(ID, PROJECT_ID, "postgresql", NAME, null, null);

      assertEquals("utf8", schema.charset());
    }

    @Test
    @DisplayName("MySQL일 때 기본 collation은 utf8mb4_general_ci")
    void defaultCollation_ForMysql() {
      Schema schema = new Schema(ID, PROJECT_ID, "mysql", NAME, null, null);

      assertEquals("utf8mb4_general_ci", schema.collation());
    }

    @Test
    @DisplayName("기타 벤더일 때 기본 collation은 utf8_general_ci")
    void defaultCollation_ForOtherVendor() {
      Schema schema = new Schema(ID, PROJECT_ID, "postgresql", NAME, null, null);

      assertEquals("utf8_general_ci", schema.collation());
    }

  }

  @Nested
  @DisplayName("검증 실패 테스트")
  class ValidationFailureTests {

    @Test
    @DisplayName("id가 blank이면 예외 발생")
    void createSchema_WithBlankId_ThrowsException() {
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> new Schema("", PROJECT_ID, "mysql", NAME, CHARSET, COLLATION));

      assertEquals("id must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("projectId가 blank이면 예외 발생")
    void createSchema_WithBlankProjectId_ThrowsException() {
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> new Schema(ID, "", "mysql", NAME, CHARSET, COLLATION));

      assertEquals("projectId must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("name이 blank이면 예외 발생")
    void createSchema_WithBlankName_ThrowsException() {
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> new Schema(ID, PROJECT_ID, "mysql", "", CHARSET, COLLATION));

      assertEquals("name must not be blank", exception.getMessage());
    }

  }

}
