package com.schemafy.core.erd.service.util;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.erd.controller.dto.response.SchemaDetailResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;
import com.schemafy.core.erd.service.util.mysql.MySqlAlterTableGenerator;
import com.schemafy.core.erd.service.util.mysql.MySqlCreateTableGenerator;
import com.schemafy.core.erd.service.util.mysql.MySqlDdlGenerator;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MySqlDdlGenerator 테스트")
class MySqlDdlGeneratorTest {

  private MySqlDdlGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new MySqlDdlGenerator(
        new MySqlCreateTableGenerator(),
        new MySqlAlterTableGenerator());
  }

  @Nested
  @DisplayName("CREATE TABLE 생성")
  class CreateTableTests {

    @Test
    @DisplayName("컬럼만 있는 테이블의 CREATE TABLE DDL을 생성한다")
    void generateCreateTable_withColumnsOnly() {
      SchemaDetailResponse schema = createSchema("test_schema");
      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "name", 2, "VARCHAR", "255", false, null),
              createColumn("col-3", "email", 3, "VARCHAR", "100", false,
                  "이메일 주소")),
          List.of(), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains("CREATE TABLE `users`");
      assertThat(ddl).contains("`id` BIGINT AUTO_INCREMENT");
      assertThat(ddl).contains("`name` VARCHAR(255)");
      assertThat(ddl).contains("`email` VARCHAR(100) COMMENT '이메일 주소'");
      assertThat(ddl).doesNotContain("PRIMARY KEY");
      assertThat(ddl).doesNotContain("FOREIGN KEY");
      assertThat(ddl).doesNotContain("UNIQUE KEY");
    }

    @Test
    @DisplayName("컬럼 순서(seqNo)에 따라 정렬된다")
    void generateCreateTable_columnsOrderedBySeqNo() {
      SchemaDetailResponse schema = createSchema("test_schema");
      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-3", "email", 3, "VARCHAR", "100", false, null),
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "name", 2, "VARCHAR", "255", false, null)),
          List.of(), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      int idPos = ddl.indexOf("`id`");
      int namePos = ddl.indexOf("`name`");
      int emailPos = ddl.indexOf("`email`");

      assertThat(idPos).isLessThan(namePos);
      assertThat(namePos).isLessThan(emailPos);
    }

  }

  @Nested
  @DisplayName("ALTER TABLE - PRIMARY KEY")
  class PrimaryKeyTests {

    @Test
    @DisplayName("단일 컬럼 PRIMARY KEY를 ALTER TABLE로 생성한다")
    void generateAlterTable_singlePrimaryKey() {
      SchemaDetailResponse schema = createSchema("test_schema");
      ConstraintResponse pkConstraint = createConstraint("pk-1", "pk_users",
          "PRIMARY_KEY",
          List.of(createConstraintColumn("pkc-1", "col-1", 1)));

      TableDetailResponse table = createTable("users",
          List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)),
          List.of(pkConstraint), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains("ALTER TABLE `users` ADD PRIMARY KEY (`id`);");
    }

    @Test
    @DisplayName("복합 PRIMARY KEY를 ALTER TABLE로 생성한다")
    void generateAlterTable_compositePrimaryKey() {
      SchemaDetailResponse schema = createSchema("test_schema");
      ConstraintResponse pkConstraint = createConstraint("pk-1",
          "pk_order_items", "PRIMARY_KEY",
          List.of(
              createConstraintColumn("pkc-1", "col-1", 1),
              createConstraintColumn("pkc-2", "col-2", 2)));

      TableDetailResponse table = createTable("order_items",
          List.of(
              createColumn("col-1", "order_id", 1, "BIGINT", null, false, null),
              createColumn("col-2", "product_id", 2, "BIGINT", null, false,
                  null)),
          List.of(pkConstraint), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains(
          "ALTER TABLE `order_items` ADD PRIMARY KEY (`order_id`, `product_id`);");
    }

  }

  @Nested
  @DisplayName("ALTER TABLE - UNIQUE")
  class UniqueConstraintTests {

    @Test
    @DisplayName("UNIQUE 제약조건을 ALTER TABLE로 생성한다")
    void generateAlterTable_uniqueConstraint() {
      SchemaDetailResponse schema = createSchema("test_schema");
      ConstraintResponse uniqueConstraint = createConstraint("uq-1",
          "uq_users_email", "UNIQUE",
          List.of(createConstraintColumn("uqc-1", "col-2", 1)));

      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "email", 2, "VARCHAR", "255", false, null)),
          List.of(uniqueConstraint), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains(
          "ALTER TABLE `users` ADD UNIQUE KEY `uq_users_email` (`email`);");
    }

    @Test
    @DisplayName("복합 UNIQUE 제약조건을 ALTER TABLE로 생성한다")
    void generateAlterTable_compositeUniqueConstraint() {
      SchemaDetailResponse schema = createSchema("test_schema");
      ConstraintResponse uniqueConstraint = createConstraint("uq-1",
          "uq_users_name_email", "UNIQUE",
          List.of(
              createConstraintColumn("uqc-1", "col-2", 1),
              createConstraintColumn("uqc-2", "col-3", 2)));

      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "name", 2, "VARCHAR", "100", false, null),
              createColumn("col-3", "email", 3, "VARCHAR", "255", false, null)),
          List.of(uniqueConstraint), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains(
          "ALTER TABLE `users` ADD UNIQUE KEY `uq_users_name_email` (`name`, `email`);");
    }

  }

  @Nested
  @DisplayName("ALTER TABLE - INDEX")
  class IndexTests {

    @Test
    @DisplayName("일반 INDEX를 ALTER TABLE로 생성한다")
    void generateAlterTable_index() {
      SchemaDetailResponse schema = createSchema("test_schema");
      IndexResponse index = createIndex("idx-1", "idx_users_name", "BTREE",
          List.of(createIndexColumn("idxc-1", "col-2", 1, "ASC")));

      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "name", 2, "VARCHAR", "100", false, null)),
          List.of(), List.of(index), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains(
          "ALTER TABLE `users` ADD INDEX `idx_users_name` (`name` ASC) USING BTREE;");
    }

    @Test
    @DisplayName("FULLTEXT INDEX를 ALTER TABLE로 생성한다")
    void generateAlterTable_fulltextIndex() {
      SchemaDetailResponse schema = createSchema("test_schema");
      IndexResponse index = createIndex("idx-1", "idx_posts_content", "FULLTEXT",
          List.of(createIndexColumn("idxc-1", "col-2", 1, null)));

      TableDetailResponse table = createTable("posts",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "content", 2, "TEXT", null, false, null)),
          List.of(), List.of(index), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains(
          "ALTER TABLE `posts` ADD FULLTEXT INDEX `idx_posts_content` (`content`);");
    }

    @Test
    @DisplayName("복합 INDEX를 ALTER TABLE로 생성한다")
    void generateAlterTable_compositeIndex() {
      SchemaDetailResponse schema = createSchema("test_schema");
      IndexResponse index = createIndex("idx-1", "idx_users_name_email", "BTREE",
          List.of(
              createIndexColumn("idxc-1", "col-2", 1, "ASC"),
              createIndexColumn("idxc-2", "col-3", 2, "DESC")));

      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "name", 2, "VARCHAR", "100", false, null),
              createColumn("col-3", "email", 3, "VARCHAR", "255", false, null)),
          List.of(), List.of(index), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains(
          "ALTER TABLE `users` ADD INDEX `idx_users_name_email` (`name` ASC, `email` DESC) USING BTREE;");
    }

    @Test
    @DisplayName("HASH INDEX를 ALTER TABLE로 생성한다")
    void generateAlterTable_hashIndex() {
      SchemaDetailResponse schema = createSchema("test_schema");
      IndexResponse index = createIndex("idx-1", "idx_users_email", "HASH",
          List.of(createIndexColumn("idxc-1", "col-2", 1, null)));

      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "email", 2, "VARCHAR", "255", false, null)),
          List.of(), List.of(index), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains(
          "ALTER TABLE `users` ADD INDEX `idx_users_email` (`email`) USING HASH;");
    }

    @Test
    @DisplayName("SPATIAL INDEX를 ALTER TABLE로 생성한다")
    void generateAlterTable_spatialIndex() {
      SchemaDetailResponse schema = createSchema("test_schema");
      IndexResponse index = createIndex("idx-1", "idx_locations_point", "SPATIAL",
          List.of(createIndexColumn("idxc-1", "col-2", 1, null)));

      TableDetailResponse table = createTable("locations",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "point", 2, "POINT", null, false, null)),
          List.of(), List.of(index), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains(
          "ALTER TABLE `locations` ADD SPATIAL INDEX `idx_locations_point` (`point`);");
    }

    @Test
    @DisplayName("sortDir가 null이면 정렬 방향이 생략된다")
    void generateAlterTable_indexWithoutSortDir() {
      SchemaDetailResponse schema = createSchema("test_schema");
      IndexResponse index = createIndex("idx-1", "idx_users_name", "BTREE",
          List.of(createIndexColumn("idxc-1", "col-2", 1, null)));

      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "name", 2, "VARCHAR", "100", false, null)),
          List.of(), List.of(index), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains(
          "ALTER TABLE `users` ADD INDEX `idx_users_name` (`name`) USING BTREE;");
      assertThat(ddl).doesNotContain("ASC");
      assertThat(ddl).doesNotContain("DESC");
    }

  }

  @Nested
  @DisplayName("ALTER TABLE - FOREIGN KEY")
  class ForeignKeyTests {

    @Test
    @DisplayName("단일 컬럼 FOREIGN KEY를 ALTER TABLE로 생성한다")
    void generateAlterTable_foreignKey() {
      SchemaDetailResponse schema = createSchema("test_schema");

      TableDetailResponse usersTable = createTable("users",
          List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)),
          List.of(), List.of(), List.of());

      RelationshipResponse fkRelationship = createRelationship("rel-1",
          "fk_orders_user_id",
          "tbl-orders", "tbl-users", "CASCADE", "CASCADE",
          List.of(createRelationshipColumn("relc-1", "col-2", "col-1", 1)));

      TableDetailResponse ordersTable = createTable("orders",
          List.of(
              createColumn("col-2", "user_id", 1, "BIGINT", null, false, null)),
          List.of(), List.of(), List.of(fkRelationship));
      ordersTable = TableDetailResponse.builder()
          .id("tbl-orders")
          .schemaId("schema-1")
          .name("orders")
          .columns(List.of(
              createColumn("col-2", "user_id", 1, "BIGINT", null, false, null)))
          .constraints(List.of())
          .indexes(List.of())
          .relationships(List.of(fkRelationship))
          .build();

      usersTable = TableDetailResponse.builder()
          .id("tbl-users")
          .schemaId("schema-1")
          .name("users")
          .columns(
              List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)))
          .constraints(List.of())
          .indexes(List.of())
          .relationships(List.of())
          .build();

      String ddl = generator.generateSchemaDdl(schema,
          List.of(usersTable, ordersTable));

      assertThat(ddl).contains(
          "ALTER TABLE `orders` ADD CONSTRAINT `fk_orders_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;");
    }

    @Test
    @DisplayName("복합 FOREIGN KEY를 ALTER TABLE로 생성한다")
    void generateAlterTable_compositeForeignKey() {
      SchemaDetailResponse schema = createSchema("test_schema");

      TableDetailResponse productsTable = TableDetailResponse.builder()
          .id("tbl-products")
          .schemaId("schema-1")
          .name("products")
          .columns(List.of(
              createColumn("col-1", "category_id", 1, "BIGINT", null, false,
                  null),
              createColumn("col-2", "product_id", 2, "BIGINT", null, false,
                  null)))
          .constraints(List.of())
          .indexes(List.of())
          .relationships(List.of())
          .build();

      RelationshipResponse fkRelationship = createRelationship("rel-1",
          "fk_order_items_product",
          "tbl-order-items", "tbl-products", "NO ACTION", "NO ACTION",
          List.of(
              createRelationshipColumn("relc-1", "col-3", "col-1", 1),
              createRelationshipColumn("relc-2", "col-4", "col-2", 2)));

      TableDetailResponse orderItemsTable = TableDetailResponse.builder()
          .id("tbl-order-items")
          .schemaId("schema-1")
          .name("order_items")
          .columns(List.of(
              createColumn("col-3", "category_id", 1, "BIGINT", null, false,
                  null),
              createColumn("col-4", "product_id", 2, "BIGINT", null, false,
                  null)))
          .constraints(List.of())
          .indexes(List.of())
          .relationships(List.of(fkRelationship))
          .build();

      String ddl = generator.generateSchemaDdl(schema,
          List.of(productsTable, orderItemsTable));

      assertThat(ddl).contains(
          "ALTER TABLE `order_items` ADD CONSTRAINT `fk_order_items_product` FOREIGN KEY (`category_id`, `product_id`) REFERENCES `products` (`category_id`, `product_id`) ON DELETE NO ACTION ON UPDATE NO ACTION;");
    }

    @Test
    @DisplayName("ON DELETE/UPDATE가 null이면 생략된다")
    void generateAlterTable_foreignKeyWithoutActions() {
      SchemaDetailResponse schema = createSchema("test_schema");

      TableDetailResponse usersTable = TableDetailResponse.builder()
          .id("tbl-users")
          .schemaId("schema-1")
          .name("users")
          .columns(
              List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)))
          .constraints(List.of())
          .indexes(List.of())
          .relationships(List.of())
          .build();

      RelationshipResponse fkRelationship = createRelationship("rel-1",
          "fk_orders_user_id",
          "tbl-orders", "tbl-users", null, null,
          List.of(createRelationshipColumn("relc-1", "col-2", "col-1", 1)));

      TableDetailResponse ordersTable = TableDetailResponse.builder()
          .id("tbl-orders")
          .schemaId("schema-1")
          .name("orders")
          .columns(List.of(
              createColumn("col-2", "user_id", 1, "BIGINT", null, false, null)))
          .constraints(List.of())
          .indexes(List.of())
          .relationships(List.of(fkRelationship))
          .build();

      String ddl = generator.generateSchemaDdl(schema,
          List.of(usersTable, ordersTable));

      assertThat(ddl).contains(
          "ALTER TABLE `orders` ADD CONSTRAINT `fk_orders_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);");
      assertThat(ddl).doesNotContain("ON DELETE");
      assertThat(ddl).doesNotContain("ON UPDATE");
    }

    @Test
    @DisplayName("ON DELETE만 있고 ON UPDATE가 null이면 ON DELETE만 생성된다")
    void generateAlterTable_foreignKeyWithOnlyOnDelete() {
      SchemaDetailResponse schema = createSchema("test_schema");

      TableDetailResponse usersTable = TableDetailResponse.builder()
          .id("tbl-users")
          .schemaId("schema-1")
          .name("users")
          .columns(
              List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)))
          .constraints(List.of())
          .indexes(List.of())
          .relationships(List.of())
          .build();

      RelationshipResponse fkRelationship = createRelationship("rel-1",
          "fk_orders_user_id",
          "tbl-orders", "tbl-users", "SET NULL", null,
          List.of(createRelationshipColumn("relc-1", "col-2", "col-1", 1)));

      TableDetailResponse ordersTable = TableDetailResponse.builder()
          .id("tbl-orders")
          .schemaId("schema-1")
          .name("orders")
          .columns(List.of(
              createColumn("col-2", "user_id", 1, "BIGINT", null, false, null)))
          .constraints(List.of())
          .indexes(List.of())
          .relationships(List.of(fkRelationship))
          .build();

      String ddl = generator.generateSchemaDdl(schema,
          List.of(usersTable, ordersTable));

      assertThat(ddl).contains("ON DELETE SET NULL");
      assertThat(ddl).doesNotContain("ON UPDATE");
    }

  }

  @Nested
  @DisplayName("DDL 순서 검증")
  class DdlOrderTests {

    @Test
    @DisplayName("CREATE TABLE이 ALTER TABLE보다 먼저 나온다")
    void ddlOrder_createTableBeforeAlterTable() {
      SchemaDetailResponse schema = createSchema("test_schema");
      ConstraintResponse pkConstraint = createConstraint("pk-1", "pk_users",
          "PRIMARY_KEY",
          List.of(createConstraintColumn("pkc-1", "col-1", 1)));

      TableDetailResponse table = createTable("users",
          List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)),
          List.of(pkConstraint), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      int createTablePos = ddl.indexOf("CREATE TABLE");
      int alterTablePos = ddl.indexOf("ALTER TABLE");

      assertThat(createTablePos).isLessThan(alterTablePos);
    }

    @Test
    @DisplayName("PRIMARY KEY가 UNIQUE보다 먼저 나온다")
    void ddlOrder_primaryKeyBeforeUnique() {
      SchemaDetailResponse schema = createSchema("test_schema");
      ConstraintResponse pkConstraint = createConstraint("pk-1", "pk_users",
          "PRIMARY_KEY",
          List.of(createConstraintColumn("pkc-1", "col-1", 1)));
      ConstraintResponse uniqueConstraint = createConstraint("uq-1",
          "uq_users_email", "UNIQUE",
          List.of(createConstraintColumn("uqc-1", "col-2", 1)));

      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, true, null),
              createColumn("col-2", "email", 2, "VARCHAR", "255", false, null)),
          List.of(pkConstraint, uniqueConstraint), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      int pkPos = ddl.indexOf("ADD PRIMARY KEY");
      int uniquePos = ddl.indexOf("ADD UNIQUE KEY");

      assertThat(pkPos).isLessThan(uniquePos);
    }

    @Test
    @DisplayName("INDEX가 FOREIGN KEY보다 먼저 나온다")
    void ddlOrder_indexBeforeForeignKey() {
      SchemaDetailResponse schema = createSchema("test_schema");

      TableDetailResponse usersTable = TableDetailResponse.builder()
          .id("tbl-users")
          .schemaId("schema-1")
          .name("users")
          .columns(
              List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)))
          .constraints(List.of())
          .indexes(List.of())
          .relationships(List.of())
          .build();

      IndexResponse index = createIndex("idx-1", "idx_orders_status", "BTREE",
          List.of(createIndexColumn("idxc-1", "col-3", 1, "ASC")));

      RelationshipResponse fkRelationship = createRelationship("rel-1",
          "fk_orders_user_id",
          "tbl-orders", "tbl-users", "CASCADE", "CASCADE",
          List.of(createRelationshipColumn("relc-1", "col-2", "col-1", 1)));

      TableDetailResponse ordersTable = TableDetailResponse.builder()
          .id("tbl-orders")
          .schemaId("schema-1")
          .name("orders")
          .columns(List.of(
              createColumn("col-2", "user_id", 1, "BIGINT", null, false, null),
              createColumn("col-3", "status", 2, "VARCHAR", "20", false, null)))
          .constraints(List.of())
          .indexes(List.of(index))
          .relationships(List.of(fkRelationship))
          .build();

      String ddl = generator.generateSchemaDdl(schema,
          List.of(usersTable, ordersTable));

      int indexPos = ddl.indexOf("ADD INDEX");
      int fkPos = ddl.indexOf("ADD CONSTRAINT");

      assertThat(indexPos).isLessThan(fkPos);
    }

    @Test
    @DisplayName("모든 CREATE TABLE이 모든 ALTER TABLE보다 먼저 나온다")
    void ddlOrder_allCreateTablesBeforeAllAlterTables() {
      SchemaDetailResponse schema = createSchema("test_schema");

      ConstraintResponse pkUsers = createConstraint("pk-1", "pk_users",
          "PRIMARY_KEY",
          List.of(createConstraintColumn("pkc-1", "col-1", 1)));

      ConstraintResponse pkOrders = createConstraint("pk-2", "pk_orders",
          "PRIMARY_KEY",
          List.of(createConstraintColumn("pkc-2", "col-2", 1)));

      TableDetailResponse usersTable = TableDetailResponse.builder()
          .id("tbl-users")
          .schemaId("schema-1")
          .name("users")
          .columns(
              List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)))
          .constraints(List.of(pkUsers))
          .indexes(List.of())
          .relationships(List.of())
          .build();

      TableDetailResponse ordersTable = TableDetailResponse.builder()
          .id("tbl-orders")
          .schemaId("schema-1")
          .name("orders")
          .columns(
              List.of(createColumn("col-2", "id", 1, "BIGINT", null, true, null)))
          .constraints(List.of(pkOrders))
          .indexes(List.of())
          .relationships(List.of())
          .build();

      String ddl = generator.generateSchemaDdl(schema,
          List.of(usersTable, ordersTable));

      int lastCreateTable = ddl.lastIndexOf("CREATE TABLE");
      int firstAlterTable = ddl.indexOf("ALTER TABLE");

      // CREATE TABLE의 끝 위치 찾기
      int createTableEndPos = ddl.indexOf(";", lastCreateTable);

      assertThat(createTableEndPos).isLessThan(firstAlterTable);
    }

  }

  @Nested
  @DisplayName("특수 케이스")
  class SpecialCasesTests {

    @Test
    @DisplayName("주석에 작은따옴표가 있으면 이스케이프된다")
    void escapeQuotesInComment() {
      SchemaDetailResponse schema = createSchema("test_schema");
      TableDetailResponse table = createTable("users",
          List.of(createColumn("col-1", "name", 1, "VARCHAR", "100", false,
              "사용자's 이름")),
          List.of(), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains("COMMENT '사용자''s 이름'");
    }

    @Test
    @DisplayName("제약조건이나 인덱스가 없으면 ALTER TABLE이 생성되지 않는다")
    void noAlterTableWhenNoConstraintsOrIndexes() {
      SchemaDetailResponse schema = createSchema("test_schema");
      TableDetailResponse table = createTable("simple_table",
          List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)),
          List.of(), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains("CREATE TABLE `simple_table`");
      assertThat(ddl).doesNotContain("ALTER TABLE");
    }

  }

  @Nested
  @DisplayName("엣지 케이스")
  class EdgeCaseTests {

    @Test
    @DisplayName("빈 테이블 리스트이면 헤더만 생성된다")
    void emptyTableList_onlyHeader() {
      SchemaDetailResponse schema = createSchema("empty_schema");

      String ddl = generator.generateSchemaDdl(schema, List.of());

      assertThat(ddl).contains("Schema: empty_schema");
      assertThat(ddl).contains("Generated by Schemafy Export");
      assertThat(ddl).doesNotContain("CREATE TABLE");
      assertThat(ddl).doesNotContain("ALTER TABLE");
    }

    @Test
    @DisplayName("스키마 이름이 헤더에 포함된다")
    void schemaNameInHeader() {
      SchemaDetailResponse schema = createSchema("my_database");
      TableDetailResponse table = createTable("users",
          List.of(createColumn("col-1", "id", 1, "BIGINT", null, true, null)),
          List.of(), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains("Schema: my_database");
    }

    @Test
    @DisplayName("컬럼의 lengthScale이 null이면 데이터타입만 출력된다")
    void columnWithoutLengthScale() {
      SchemaDetailResponse schema = createSchema("test_schema");
      TableDetailResponse table = createTable("users",
          List.of(
              createColumn("col-1", "id", 1, "BIGINT", null, false, null),
              createColumn("col-2", "created_at", 2, "TIMESTAMP", null, false,
                  null)),
          List.of(), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains("`id` BIGINT");
      assertThat(ddl).contains("`created_at` TIMESTAMP");
      assertThat(ddl).doesNotContain("BIGINT(");
      assertThat(ddl).doesNotContain("TIMESTAMP(");
    }

    @Test
    @DisplayName("컬럼의 lengthScale이 빈 문자열이면 데이터타입만 출력된다")
    void columnWithEmptyLengthScale() {
      SchemaDetailResponse schema = createSchema("test_schema");
      TableDetailResponse table = createTable("users",
          List.of(createColumn("col-1", "id", 1, "INT", "", false, null)),
          List.of(), List.of(), List.of());

      String ddl = generator.generateSchemaDdl(schema, List.of(table));

      assertThat(ddl).contains("`id` INT");
      assertThat(ddl).doesNotContain("INT(");
    }

  }

  // Helper methods
  private SchemaDetailResponse createSchema(String name) {
    return SchemaDetailResponse.builder()
        .id("schema-1")
        .name(name)
        .tables(List.of())
        .build();
  }

  private TableDetailResponse createTable(String name,
      List<ColumnResponse> columns,
      List<ConstraintResponse> constraints,
      List<IndexResponse> indexes,
      List<RelationshipResponse> relationships) {
    return TableDetailResponse.builder()
        .id("tbl-" + name)
        .schemaId("schema-1")
        .name(name)
        .columns(columns)
        .constraints(constraints)
        .indexes(indexes)
        .relationships(relationships)
        .build();
  }

  private ColumnResponse createColumn(String id, String name, int seqNo,
      String dataType, String lengthScale, boolean isAutoIncrement,
      String comment) {
    return ColumnResponse.builder()
        .id(id)
        .name(name)
        .seqNo(seqNo)
        .dataType(dataType)
        .lengthScale(lengthScale)
        .isAutoIncrement(isAutoIncrement)
        .comment(comment)
        .build();
  }

  private ConstraintResponse createConstraint(String id, String name,
      String kind,
      List<ConstraintColumnResponse> columns) {
    return ConstraintResponse.builder()
        .id(id)
        .name(name)
        .kind(kind)
        .columns(columns)
        .build();
  }

  private ConstraintColumnResponse createConstraintColumn(String id,
      String columnId, int seqNo) {
    return ConstraintColumnResponse.builder()
        .id(id)
        .columnId(columnId)
        .seqNo(seqNo)
        .build();
  }

  private IndexResponse createIndex(String id, String name, String type,
      List<IndexColumnResponse> columns) {
    return IndexResponse.builder()
        .id(id)
        .name(name)
        .type(type)
        .columns(columns)
        .build();
  }

  private IndexColumnResponse createIndexColumn(String id, String columnId,
      int seqNo, String sortDir) {
    return IndexColumnResponse.builder()
        .id(id)
        .columnId(columnId)
        .seqNo(seqNo)
        .sortDir(sortDir)
        .build();
  }

  private RelationshipResponse createRelationship(String id, String name,
      String fkTableId, String pkTableId, String onDelete, String onUpdate,
      List<RelationshipColumnResponse> columns) {
    return RelationshipResponse.builder()
        .id(id)
        .name(name)
        .fkTableId(fkTableId)
        .pkTableId(pkTableId)
        .onDelete(onDelete)
        .onUpdate(onUpdate)
        .columns(columns)
        .build();
  }

  private RelationshipColumnResponse createRelationshipColumn(String id,
      String fkColumnId, String pkColumnId, int seqNo) {
    return RelationshipColumnResponse.builder()
        .id(id)
        .fkColumnId(fkColumnId)
        .pkColumnId(pkColumnId)
        .seqNo(seqNo)
        .build();
  }

}
