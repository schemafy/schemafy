package com.schemafy.api.erd.service.util.mysql;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.api.erd.controller.dto.response.ColumnResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.api.erd.controller.dto.response.IndexResponse;
import com.schemafy.api.erd.controller.dto.response.IndexSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.domain.type.SortDirection;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MySqlDdlGenerator 테스트")
class MySqlDdlGeneratorTest {

  private MySqlDdlGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new MySqlDdlGenerator(
        new MySqlCreateTableGenerator(),
        new MySqlAlterTableGenerator(
            new MySqlPrimaryKeyGenerator(),
            new MySqlUniqueKeyGenerator(),
            new MySqlIndexGenerator(),
            new MySqlForeignKeyGenerator()));
  }

  @Nested
  @DisplayName("CREATE TABLE 생성")
  class CreateTableTest {

    private MySqlCreateTableGenerator createTableGenerator;

    @BeforeEach
    void setUp() {
      createTableGenerator = new MySqlCreateTableGenerator();
    }

    @Test
    @DisplayName("기본 컬럼으로 CREATE TABLE 생성")
    void generate_basicColumns() {
      ColumnResponse col1 = new ColumnResponse(
          "col1", "t1", "id", "BIGINT", null, 1, true, null, null, null);
      ColumnResponse col2 = new ColumnResponse(
          "col2", "t1", "name", "VARCHAR",
          new ColumnTypeArguments(255, null, null), 2, false, null, null, null);

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", "utf8mb4", "utf8mb4_unicode_ci", null),
          List.of(col1, col2), null, null, null);

      String result = createTableGenerator.generate(table);

      assertThat(result).contains("CREATE TABLE `users`");
      assertThat(result).contains("`id` BIGINT AUTO_INCREMENT");
      assertThat(result).contains("`name` VARCHAR(255)");
      assertThat(result).contains("ENGINE=InnoDB");
      assertThat(result).contains("DEFAULT CHARSET=utf8mb4");
      assertThat(result).contains("COLLATE=utf8mb4_unicode_ci");
    }

    @Test
    @DisplayName("DECIMAL 타입 precision/scale 지원")
    void generate_decimalType() {
      ColumnResponse col = new ColumnResponse(
          "col1", "t1", "price", "DECIMAL",
          new ColumnTypeArguments(null, 10, 2), 1, false, null, null, null);

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "products", null, null, null),
          List.of(col), null, null, null);

      String result = createTableGenerator.generate(table);

      assertThat(result).contains("`price` DECIMAL(10,2)");
    }

    @Test
    @DisplayName("ENUM 타입 values 지원")
    void generate_enumType() {
      ColumnResponse col = new ColumnResponse(
          "col1", "t1", "status", "ENUM",
          new ColumnTypeArguments(null, null, null, List.of("ACTIVE", "INACTIVE")),
          1, false, null, null, null);

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", null, null, null),
          List.of(col), null, null, null);

      String result = createTableGenerator.generate(table);

      assertThat(result).contains("`status` ENUM('ACTIVE', 'INACTIVE')");
    }

    @Test
    @DisplayName("컬럼 코멘트 생성")
    void generate_withComment() {
      ColumnResponse col = new ColumnResponse(
          "col1", "t1", "name", "VARCHAR",
          new ColumnTypeArguments(100, null, null), 1, false, null, null, "사용자 이름");

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", null, null, null),
          List.of(col), null, null, null);

      String result = createTableGenerator.generate(table);

      assertThat(result).contains("COMMENT '사용자 이름'");
    }

    @Test
    @DisplayName("컬럼 charset/collation 생성")
    void generate_withCharsetAndCollation() {
      ColumnResponse col = new ColumnResponse(
          "col1", "t1", "name", "VARCHAR",
          new ColumnTypeArguments(100, null, null), 1, false, "utf8", "utf8_general_ci", null);

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", null, null, null),
          List.of(col), null, null, null);

      String result = createTableGenerator.generate(table);

      assertThat(result).contains("CHARACTER SET utf8");
      assertThat(result).contains("COLLATE utf8_general_ci");
    }

    @Test
    @DisplayName("컬럼 없는 테이블 - 예외 발생")
    void generate_emptyColumns_throws() {
      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", null, null, null),
          Collections.emptyList(), null, null, null);

      assertThatThrownBy(() -> createTableGenerator.generate(table))
          .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("seqNo 기반 컬럼 정렬")
    void generate_columnsOrderedBySeqNo() {
      ColumnResponse col1 = new ColumnResponse(
          "col1", "t1", "name", "VARCHAR", null, 2, false, null, null, null);
      ColumnResponse col2 = new ColumnResponse(
          "col2", "t1", "id", "BIGINT", null, 1, true, null, null, null);

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", null, null, null),
          List.of(col1, col2), null, null, null);

      String result = createTableGenerator.generate(table);

      int idPos = result.indexOf("`id`");
      int namePos = result.indexOf("`name`");
      assertThat(idPos).isLessThan(namePos);
    }

    @Test
    @DisplayName("특수문자 이스케이핑")
    void generate_specialCharacterEscaping() {
      ColumnResponse col = new ColumnResponse(
          "col1", "t1", "user`name", "VARCHAR", null, 1, false, null, null, "it's a test");

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "my`table", null, null, null),
          List.of(col), null, null, null);

      String result = createTableGenerator.generate(table);

      assertThat(result).contains("CREATE TABLE `my``table`");
      assertThat(result).contains("`user``name`");
      assertThat(result).contains("COMMENT 'it''s a test'");
    }

  }

  @Nested
  @DisplayName("INDEX 생성")
  class IndexTest {

    private MySqlIndexGenerator indexGenerator;

    @BeforeEach
    void setUp() {
      indexGenerator = new MySqlIndexGenerator();
    }

    @Test
    @DisplayName("BTREE 인덱스 생성")
    void generate_btreeIndex() {
      IndexColumnResponse idxCol = new IndexColumnResponse("ic1", "idx1", "col1", 1, SortDirection.ASC);
      IndexSnapshotResponse idx = new IndexSnapshotResponse(
          new IndexResponse("idx1", "t1", "idx_name", IndexType.BTREE),
          List.of(idxCol));

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", null, null, null),
          null, null, null, List.of(idx));

      var result = indexGenerator.generate(table, java.util.Map.of("col1", "name"));

      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo(
          "ALTER TABLE `users` ADD INDEX `idx_name` (`name` ASC) USING BTREE;");
    }

    @Test
    @DisplayName("FULLTEXT 인덱스 생성")
    void generate_fulltextIndex() {
      IndexColumnResponse idxCol = new IndexColumnResponse("ic1", "idx1", "col1", 1, null);
      IndexSnapshotResponse idx = new IndexSnapshotResponse(
          new IndexResponse("idx1", "t1", "idx_content", IndexType.FULLTEXT),
          List.of(idxCol));

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "posts", null, null, null),
          null, null, null, List.of(idx));

      var result = indexGenerator.generate(table, java.util.Map.of("col1", "content"));

      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo(
          "ALTER TABLE `posts` ADD FULLTEXT INDEX `idx_content` (`content`);");
    }

    @Test
    @DisplayName("복합 인덱스 컬럼 순서")
    void generate_compositeIndex() {
      IndexColumnResponse ic1 = new IndexColumnResponse("ic1", "idx1", "col1", 2, SortDirection.DESC);
      IndexColumnResponse ic2 = new IndexColumnResponse("ic2", "idx1", "col2", 1, SortDirection.ASC);

      IndexSnapshotResponse idx = new IndexSnapshotResponse(
          new IndexResponse("idx1", "t1", "idx_composite", IndexType.BTREE),
          List.of(ic1, ic2));

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "orders", null, null, null),
          null, null, null, List.of(idx));

      var result = indexGenerator.generate(table, java.util.Map.of("col1", "price", "col2", "date"));

      assertThat(result).hasSize(1);
      assertThat(result.get(0)).contains("`date` ASC, `price` DESC");
    }

    @Test
    @DisplayName("HASH 인덱스 생성")
    void generate_hashIndex() {
      IndexColumnResponse idxCol = new IndexColumnResponse("ic1", "idx1", "col1", 1, null);
      IndexSnapshotResponse idx = new IndexSnapshotResponse(
          new IndexResponse("idx1", "t1", "idx_hash", IndexType.HASH),
          List.of(idxCol));

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "cache", null, null, null),
          null, null, null, List.of(idx));

      var result = indexGenerator.generate(table, java.util.Map.of("col1", "key"));

      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo(
          "ALTER TABLE `cache` ADD INDEX `idx_hash` (`key`) USING HASH;");
    }

  }

  @Nested
  @DisplayName("전체 DDL 생성")
  class FullDdlTest {

    @Test
    @DisplayName("스키마 전체 DDL 생성 - CREATE TABLE + ALTER TABLE")
    void generateSchemaDdl_fullSchema() {
      ColumnResponse usersCol1 = new ColumnResponse(
          "col1", "t1", "id", "BIGINT", null, 1, true, null, null, null);
      ColumnResponse usersCol2 = new ColumnResponse(
          "col2", "t1", "email", "VARCHAR",
          new ColumnTypeArguments(255, null, null), 2, false, null, null, null);

      ConstraintSnapshotResponse usersPk = new ConstraintSnapshotResponse(
          new ConstraintResponse("pk1", "t1", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
          List.of(new ConstraintColumnResponse("cc1", "pk1", "col1", 1)));

      ConstraintSnapshotResponse usersUk = new ConstraintSnapshotResponse(
          new ConstraintResponse("uk1", "t1", "uk_email", ConstraintKind.UNIQUE, null, null),
          List.of(new ConstraintColumnResponse("cc2", "uk1", "col2", 1)));

      TableSnapshotResponse usersTable = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", "utf8mb4", "utf8mb4_unicode_ci", null),
          List.of(usersCol1, usersCol2),
          List.of(usersPk, usersUk),
          null, null);

      ColumnResponse ordersCol1 = new ColumnResponse(
          "col3", "t2", "id", "BIGINT", null, 1, true, null, null, null);
      ColumnResponse ordersCol2 = new ColumnResponse(
          "col4", "t2", "user_id", "BIGINT", null, 2, false, null, null, null);

      ConstraintSnapshotResponse ordersPk = new ConstraintSnapshotResponse(
          new ConstraintResponse("pk2", "t2", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
          List.of(new ConstraintColumnResponse("cc3", "pk2", "col3", 1)));

      RelationshipSnapshotResponse fk = new RelationshipSnapshotResponse(
          new RelationshipResponse("fk1", "t2", "t1", "fk_user_id",
              RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null),
          List.of(new RelationshipColumnResponse("rc1", "fk1", "col1", "col4", 1)));

      TableSnapshotResponse ordersTable = new TableSnapshotResponse(
          new TableResponse("t2", "s1", "orders", "utf8mb4", "utf8mb4_unicode_ci", null),
          List.of(ordersCol1, ordersCol2),
          List.of(ordersPk),
          List.of(fk), null);

      SchemaResponse schema = new SchemaResponse(
          "s1", "p1", "mysql", "my_schema", "utf8mb4", "utf8mb4_unicode_ci", null);

      String ddl = generator.generateSchemaDdl(schema, List.of(usersTable, ordersTable));

      assertThat(ddl).contains("-- Schema: my_schema");
      assertThat(ddl).contains("CREATE TABLE `users`");
      assertThat(ddl).contains("CREATE TABLE `orders`");
      assertThat(ddl).contains("`id` BIGINT AUTO_INCREMENT");
      assertThat(ddl).contains("`email` VARCHAR(255)");
      assertThat(ddl).contains("ALTER TABLE `users` ADD PRIMARY KEY (`id`);");
      assertThat(ddl).contains("ALTER TABLE `users` ADD UNIQUE KEY `uk_email` (`email`);");
      assertThat(ddl).contains("ALTER TABLE `orders` ADD PRIMARY KEY (`id`);");
      assertThat(ddl).contains(
          "ALTER TABLE `orders` ADD CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);");
    }

    @Test
    @DisplayName("테이블과 ALTER 문 순서는 이름 기준으로 고정된다")
    void generateSchemaDdl_ordersTablesAndAlterStatementsDeterministically() {
      ColumnResponse userId = new ColumnResponse(
          "user-col-1", "t-users", "id", "BIGINT", null, 1, true, null, null, null);
      ColumnResponse orderId = new ColumnResponse(
          "order-col-1", "t-orders", "id", "BIGINT", null, 1, true, null, null, null);
      ColumnResponse orderUserId = new ColumnResponse(
          "order-col-2", "t-orders", "user_id", "BIGINT", null, 2, false, null, null, null);

      ConstraintSnapshotResponse usersPk = new ConstraintSnapshotResponse(
          new ConstraintResponse("pk-users", "t-users", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
          List.of(new ConstraintColumnResponse("pk-users-col", "pk-users", "user-col-1", 1)));

      ConstraintSnapshotResponse ordersPk = new ConstraintSnapshotResponse(
          new ConstraintResponse("pk-orders", "t-orders", "PRIMARY", ConstraintKind.PRIMARY_KEY, null, null),
          List.of(new ConstraintColumnResponse("pk-orders-col", "pk-orders", "order-col-1", 1)));

      IndexSnapshotResponse idxOrdersUser = new IndexSnapshotResponse(
          new IndexResponse("idx-orders-user", "t-orders", "idx_orders_user", IndexType.BTREE),
          List.of(new IndexColumnResponse("idx-orders-user-col", "idx-orders-user", "order-col-2", 1,
              SortDirection.ASC)));

      RelationshipSnapshotResponse fkOrdersUser = new RelationshipSnapshotResponse(
          new RelationshipResponse("fk-orders-user", "t-orders", "t-users", "fk_orders_user",
              RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null),
          List.of(new RelationshipColumnResponse("fk-orders-user-col", "fk-orders-user", "user-col-1", "order-col-2",
              1)));

      TableSnapshotResponse usersTable = new TableSnapshotResponse(
          new TableResponse("t-users", "s1", "users", null, null, null),
          List.of(userId),
          List.of(usersPk),
          null,
          null);

      TableSnapshotResponse ordersTable = new TableSnapshotResponse(
          new TableResponse("t-orders", "s1", "orders", null, null, null),
          List.of(orderId, orderUserId),
          List.of(ordersPk),
          List.of(fkOrdersUser),
          List.of(idxOrdersUser));

      SchemaResponse schema = new SchemaResponse(
          "s1", "p1", "mysql", "ordered_schema", null, null, null);

      String ddl = generator.generateSchemaDdl(schema, List.of(usersTable, ordersTable));

      assertThat(ddl.indexOf("CREATE TABLE `orders`"))
          .isLessThan(ddl.indexOf("CREATE TABLE `users`"));
      assertThat(ddl.indexOf("ALTER TABLE `orders` ADD PRIMARY KEY (`id`);"))
          .isLessThan(ddl.indexOf("ALTER TABLE `orders` ADD INDEX `idx_orders_user` (`user_id` ASC) USING BTREE;"));
      assertThat(ddl.indexOf("ALTER TABLE `orders` ADD INDEX `idx_orders_user` (`user_id` ASC) USING BTREE;"))
          .isLessThan(ddl.indexOf("ALTER TABLE `users` ADD PRIMARY KEY (`id`);"));
      assertThat(ddl.indexOf(
          "ALTER TABLE `users` ADD PRIMARY KEY (`id`);"))
          .isLessThan(ddl.indexOf(
              "ALTER TABLE `orders` ADD CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);"));
    }

    @Test
    @DisplayName("빈 테이블 리스트 - 헤더만 생성")
    void generateSchemaDdl_emptyTables() {
      SchemaResponse schema = new SchemaResponse(
          "s1", "p1", "mysql", "empty_schema", null, null, null);

      String ddl = generator.generateSchemaDdl(schema, Collections.emptyList());

      assertThat(ddl).contains("-- Schema: empty_schema");
      assertThat(ddl).doesNotContain("CREATE TABLE");
      assertThat(ddl).doesNotContain("ALTER TABLE");
    }

    @Test
    @DisplayName("null 테이블 리스트 처리")
    void generateSchemaDdl_nullTables() {
      SchemaResponse schema = new SchemaResponse(
          "s1", "p1", "mysql", "test", null, null, null);

      String ddl = generator.generateSchemaDdl(schema, null);

      assertThat(ddl).contains("-- Schema: test");
      assertThat(ddl).doesNotContain("CREATE TABLE");
    }

    @Test
    @DisplayName("null 테이블 엔트리는 무시하고 나머지 DDL을 생성한다")
    void generateSchemaDdl_ignoresNullTableEntries() {
      ColumnResponse col = new ColumnResponse(
          "col1", "t1", "id", "BIGINT", null, 1, true, null, null, null);

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", null, null, null),
          List.of(col), null, null, null);

      SchemaResponse schema = new SchemaResponse(
          "s1", "p1", "mysql", "test", null, null, null);

      String ddl = generator.generateSchemaDdl(schema,
          java.util.Arrays.asList(null, table));

      assertThat(ddl).contains("CREATE TABLE `users`");
    }

    @Test
    @DisplayName("인덱스 포함 전체 DDL")
    void generateSchemaDdl_withIndexes() {
      ColumnResponse col1 = new ColumnResponse(
          "col1", "t1", "id", "BIGINT", null, 1, true, null, null, null);
      ColumnResponse col2 = new ColumnResponse(
          "col2", "t1", "name", "VARCHAR", null, 2, false, null, null, null);

      IndexSnapshotResponse idx = new IndexSnapshotResponse(
          new IndexResponse("idx1", "t1", "idx_name", IndexType.BTREE),
          List.of(new IndexColumnResponse("ic1", "idx1", "col2", 1, SortDirection.ASC)));

      TableSnapshotResponse table = new TableSnapshotResponse(
          new TableResponse("t1", "s1", "users", null, null, null),
          List.of(col1, col2), null, null, List.of(idx));

      SchemaResponse schema = new SchemaResponse(
          "s1", "p1", "mysql", "test", null, null, null);

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains("CREATE TABLE `users`");
      assertThat(ddl).contains("ALTER TABLE `users` ADD INDEX `idx_name` (`name` ASC) USING BTREE;");
    }

    @Test
    @DisplayName("스키마명 SQL 코멘트 이스케이핑")
    void generateSchemaDdl_escapesSchemaNameInComment() {
      SchemaResponse schema = new SchemaResponse(
          "s1", "p1", "mysql", "test--injection", null, null, null);

      String ddl = generator.generateSchemaDdl(schema, Collections.emptyList());

      assertThat(ddl).contains("-- Schema: test- -injection");
      assertThat(ddl).doesNotContain("test--injection");
    }

  }

}
