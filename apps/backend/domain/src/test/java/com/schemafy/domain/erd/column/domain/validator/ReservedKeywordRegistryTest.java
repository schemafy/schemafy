package com.schemafy.domain.erd.column.domain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReservedKeywordRegistry")
class ReservedKeywordRegistryTest {

  @Nested
  @DisplayName("isReserved 메서드는")
  class IsReserved {

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT", "INSERT", "UPDATE", "DELETE", "FROM", "WHERE",
        "JOIN", "ORDER", "GROUP", "HAVING", "TABLE", "CREATE",
        "INDEX", "KEY", "PRIMARY", "FOREIGN", "REFERENCES", "CONSTRAINT",
        "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "IS", "NULL",
        "INT", "VARCHAR", "TEXT", "DATE", "DATETIME", "BOOLEAN", "JSON"
    })
    @DisplayName("MySQL 예약어면 true를 반환한다 (대문자)")
    void returnsTrueForMySqlReservedKeywordsUppercase(String keyword) {
      assertThat(ReservedKeywordRegistry.isReserved("mysql", keyword)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "select", "insert", "update", "delete", "from", "where",
        "join", "order", "group", "having", "table", "create"
    })
    @DisplayName("MySQL 예약어면 true를 반환한다 (소문자)")
    void returnsTrueForMySqlReservedKeywordsLowercase(String keyword) {
      assertThat(ReservedKeywordRegistry.isReserved("mysql", keyword)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "Select", "INSERT", "update" })
    @DisplayName("대소문자를 무시하고 검사한다")
    void ignoresCase(String keyword) {
      assertThat(ReservedKeywordRegistry.isReserved("mysql", keyword)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "column_name", "user_select", "order_id", "table_name",
        "my_column", "customer", "product", "amount"
    })
    @DisplayName("예약어가 아니면 false를 반환한다")
    void returnsFalseForNonReservedKeywords(String name) {
      assertThat(ReservedKeywordRegistry.isReserved("mysql", name)).isFalse();
    }

    @Test
    @DisplayName("null 이름이면 false를 반환한다")
    void returnsFalseForNullName() {
      assertThat(ReservedKeywordRegistry.isReserved("mysql", null)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t" })
    @DisplayName("빈 이름이면 false를 반환한다")
    void returnsFalseForBlankName(String name) {
      assertThat(ReservedKeywordRegistry.isReserved("mysql", name)).isFalse();
    }

    @Test
    @DisplayName("공백이 있는 이름은 trim 후 검사한다")
    void trimsWhitespace() {
      assertThat(ReservedKeywordRegistry.isReserved("mysql", "  SELECT  ")).isTrue();
      assertThat(ReservedKeywordRegistry.isReserved("mysql", "  column_name  ")).isFalse();
    }

  }

  @Nested
  @DisplayName("getKeywords 메서드는")
  class GetKeywords {

    @Test
    @DisplayName("MySQL 벤더에 대해 예약어 목록을 반환한다")
    void returnsMySqlKeywordsForMySql() {
      var keywords = ReservedKeywordRegistry.getKeywords("mysql");

      assertThat(keywords)
          .isNotEmpty()
          .contains("SELECT", "INSERT", "UPDATE", "DELETE", "TABLE", "INDEX");
    }

    @Test
    @DisplayName("MariaDB 벤더에 대해 MySQL과 동일한 예약어 목록을 반환한다")
    void returnsMariaDbKeywordsSameAsMySql() {
      var mySqlKeywords = ReservedKeywordRegistry.getKeywords("mysql");
      var mariaDbKeywords = ReservedKeywordRegistry.getKeywords("mariadb");

      assertThat(mariaDbKeywords).isEqualTo(mySqlKeywords);
    }

    @Test
    @DisplayName("알 수 없는 벤더에 대해 MySQL 예약어를 기본값으로 반환한다")
    void returnsMySqlKeywordsAsDefaultForUnknownVendor() {
      var mySqlKeywords = ReservedKeywordRegistry.getKeywords("mysql");
      var unknownKeywords = ReservedKeywordRegistry.getKeywords("unknown_db");

      assertThat(unknownKeywords).isEqualTo(mySqlKeywords);
    }

    @Test
    @DisplayName("null 벤더에 대해 MySQL 예약어를 기본값으로 반환한다")
    void returnsMySqlKeywordsAsDefaultForNullVendor() {
      var mySqlKeywords = ReservedKeywordRegistry.getKeywords("mysql");
      var nullKeywords = ReservedKeywordRegistry.getKeywords(null);

      assertThat(nullKeywords).isEqualTo(mySqlKeywords);
    }

    @Test
    @DisplayName("벤더 이름은 대소문자를 무시한다")
    void ignoresCaseForVendorName() {
      var lowerCase = ReservedKeywordRegistry.getKeywords("mysql");
      var upperCase = ReservedKeywordRegistry.getKeywords("MYSQL");
      var mixedCase = ReservedKeywordRegistry.getKeywords("MySQL");

      assertThat(upperCase).isEqualTo(lowerCase);
      assertThat(mixedCase).isEqualTo(lowerCase);
    }

  }

  @Nested
  @DisplayName("벤더별 예약어 검사는")
  class VendorSpecificReservedKeywords {

    @Test
    @DisplayName("MySQL에서 INDEX는 예약어이다")
    void indexIsReservedInMySql() {
      assertThat(ReservedKeywordRegistry.isReserved("mysql", "INDEX")).isTrue();
    }

    @Test
    @DisplayName("MariaDB에서 INDEX는 예약어이다")
    void indexIsReservedInMariaDb() {
      assertThat(ReservedKeywordRegistry.isReserved("mariadb", "INDEX")).isTrue();
    }

    @Test
    @DisplayName("알 수 없는 벤더에서도 MySQL 기준으로 INDEX는 예약어이다")
    void indexIsReservedInUnknownVendorUsingMySqlDefault() {
      assertThat(ReservedKeywordRegistry.isReserved("postgresql", "INDEX")).isTrue();
    }

  }

}
