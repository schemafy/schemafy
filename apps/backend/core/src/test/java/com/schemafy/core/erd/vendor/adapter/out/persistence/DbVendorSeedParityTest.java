package com.schemafy.core.erd.vendor.adapter.out.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DB vendor seed datatype policy")
class DbVendorSeedParityTest {

  private static final Pattern DATATYPE_POLICY_LITERAL = Pattern.compile(
      "(?s)'8\\.0',\\s*'(\\{.*?\\})',\\s*'\\{\\s*\"schemaVersion\"");

  private final JsonCodec jsonCodec = new JsonCodec(
      new ObjectMapper().findAndRegisterModules());

  @Test
  @DisplayName("H2와 MariaDB seed는 동일한 schema version 2 policy를 선언한다")
  void seedsContainSameSchemaVersionTwoPolicy() throws IOException {
    DatatypePolicy h2 = readPolicy("ddl/h2/db_vendors_data.sql");
    DatatypePolicy mariadb = readPolicy("ddl/mariadb/db_vendors_data.sql");

    assertThat(h2).isEqualTo(mariadb);
    assertThat(h2.schemaVersion()).isEqualTo(2);
    assertThat(h2.versionRange()).isEqualTo(">= 8.0 < 9.0");
    assertThat(h2.find("INTEGER").orElseThrow().sqlType()).isEqualTo("INT");
    assertThat(h2.find("GEOMETRYCOLLECTION")).isPresent();
  }

  private DatatypePolicy readPolicy(String resourcePath) throws IOException {
    String sql;
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (input == null) {
        throw new IllegalStateException("Missing test resource: " + resourcePath);
      }
      sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    Matcher matcher = DATATYPE_POLICY_LITERAL.matcher(sql);
    if (!matcher.find()) {
      throw new IllegalStateException("Datatype policy SQL literal not found: " + resourcePath);
    }
    return jsonCodec.fromJson(matcher.group(1).replace("''", "'"), DatatypePolicy.class);
  }

}
