package com.schemafy.core.erd.ddl.domain.mysql;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.ddl.domain.exception.DdlErrorCode;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Column;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Constraint;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.ConstraintColumn;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.ConstraintSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Index;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.IndexColumn;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.IndexSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Relationship;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.RelationshipColumn;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.RelationshipSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.SchemaSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Table;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.TableSnapshot;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.domain.type.SortDirection;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MySqlDdlGenerator")
class MySqlDdlGeneratorTest {

  private final MySqlDdlGenerator sut = new MySqlDdlGenerator();

  @Test
  @DisplayName("schema/table/column/constraint/index/relationship snapshot으로 MySQL DDL을 생성한다")
  void generateFullSchemaDdl() {
    SchemaExportSnapshot snapshot = schema(
        userTable(),
        orderTable());

    String ddl = sut.generate(snapshot);

    assertThat(ddl).contains(
        "-- Schemafy MySQL DDL Export",
        "-- Schema: app`schema",
        "CREATE SCHEMA IF NOT EXISTS `app``schema` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
        "USE `app``schema`;");
    assertThat(ddl).contains(
        "CREATE TABLE `orders`",
        "  `id` BIGINT NOT NULL AUTO_INCREMENT",
        "  `user_id` BIGINT NOT NULL",
        "  `status` ENUM('READY', 'it\\\\''s') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'READY' COMMENT 'order''s status'",
        "  PRIMARY KEY (`id`)",
        "  CONSTRAINT `ck_order_total` CHECK (total >= 0)",
        "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    assertThat(ddl).contains(
        "CREATE TABLE `users`",
        "  `email` VARCHAR(255) NOT NULL",
        "  PRIMARY KEY (`id`)",
        "ALTER TABLE `users` ADD UNIQUE KEY `uk_users_email` (`email`);",
        "ALTER TABLE `orders` ADD INDEX `idx_orders_user` (`user_id` ASC) USING BTREE;",
        "ALTER TABLE `orders` ADD CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;");
    assertThat(ddl.indexOf("CREATE TABLE `orders`"))
        .isLessThan(ddl.indexOf("CREATE TABLE `users`"));
    assertThat(ddl.indexOf("ALTER TABLE `orders` ADD INDEX"))
        .isLessThan(ddl.indexOf("ALTER TABLE `orders` ADD CONSTRAINT"));
  }

  @Test
  @DisplayName("reserved keyword identifier와 문자열 literal 특수문자를 MySQL DDL로 안전하게 출력한다")
  void generatesQuotedIdentifiersAndEscapedStringLiterals() {
    TableSnapshot table = new TableSnapshot(
        table("select", "select"),
        List.of(
            column("c_order", "select", "order", "VARCHAR",
                new ColumnTypeArguments(50, null, null), 0, false,
                null, null, "quote ' and slash \\ and newline\n"),
            column("c_status", "select", "status", "VARCHAR",
                new ColumnTypeArguments(20, null, null), 1, false),
            column("c_created_at", "select", "created_at", "DATETIME",
                null, 2, false)),
        List.of(
            defaultConstraint("df-order", "select", "c_order",
                "'O''Reilly\\\\books'"),
            defaultConstraint("df-created-at", "select", "c_created_at",
                "(CURRENT_TIMESTAMP)"),
            constraint("ck-status", "select", "ck_select_status",
                ConstraintKind.CHECK, "status IN ('READY', 'DONE')", null)),
        List.of(),
        List.of());

    String ddl = sut.generate(schema(table));

    assertThat(ddl).contains(
        "CREATE TABLE `select`",
        "  `order` VARCHAR(50) DEFAULT 'O''Reilly\\\\books' COMMENT 'quote '' and slash \\\\ and newline\\n'",
        "  `created_at` DATETIME DEFAULT (CURRENT_TIMESTAMP)",
        "  CONSTRAINT `ck_select_status` CHECK (status IN ('READY', 'DONE'))");
  }

  @Test
  @DisplayName("identifier가 MySQL 최대 길이인 64자를 초과하면 예외가 발생한다")
  void throwsWhenIdentifierExceedsMysqlLengthLimit() {
    TableSnapshot table = new TableSnapshot(
        table("t1", "a".repeat(65)),
        List.of(column("c1", "t1", "id", "BIGINT", null, 0, false)),
        List.of(),
        List.of(),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(table)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("self-referencing FK를 CREATE TABLE 이후 ALTER TABLE로 생성한다")
  void generatesSelfReferencingForeignKeyAfterCreateTable() {
    RelationshipSnapshot relationship = new RelationshipSnapshot(
        new Relationship(
            "r-employees-manager", "employees", "employees",
            "fk_employees_manager",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY,
            "SET NULL", "CASCADE"),
        List.of(new RelationshipColumn(
            "rc-employees-manager", "r-employees-manager",
            "employee_id", "manager_id", 0)));
    TableSnapshot employees = new TableSnapshot(
        table("employees", "employees"),
        List.of(
            column("employee_id", "employees", "id", "BIGINT", null, 0,
                false),
            column("manager_id", "employees", "manager_id", "BIGINT",
                null, 1, false)),
        List.of(pk("pk-employees", "employees", "employee_id")),
        List.of(relationship),
        List.of(new IndexSnapshot(
            new Index("idx-employees-manager", "employees",
                "idx_employees_manager", IndexType.BTREE),
            List.of(new IndexColumn("ic-employees-manager",
                "idx-employees-manager", "manager_id", 0,
                SortDirection.ASC)))));

    String ddl = sut.generate(schema(employees));

    assertThat(ddl).contains(
        "CREATE TABLE `employees`",
        "ALTER TABLE `employees` ADD INDEX `idx_employees_manager` (`manager_id` ASC) USING BTREE;",
        "ALTER TABLE `employees` ADD CONSTRAINT `fk_employees_manager` FOREIGN KEY (`manager_id`) REFERENCES `employees` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;");
    assertThat(ddl.indexOf("CREATE TABLE `employees`"))
        .isLessThan(ddl.indexOf("ALTER TABLE `employees` ADD CONSTRAINT"));
  }

  @Test
  @DisplayName("composite FK는 relationship column seqNo 순서대로 FK/PK 컬럼을 생성한다")
  void generatesCompositeForeignKeyInSeqNoOrder() {
    TableSnapshot subscriptions = new TableSnapshot(
        table("subscriptions", "subscriptions"),
        List.of(
            column("subscription_tenant", "subscriptions", "tenant_id",
                "BIGINT", null, 0, false),
            column("subscription_code", "subscriptions", "code",
                "VARCHAR", new ColumnTypeArguments(30, null, null), 1,
                false)),
        List.of(pk("pk-subscriptions", "subscriptions",
            "subscription_tenant", "subscription_code")),
        List.of(),
        List.of());
    RelationshipSnapshot relationship = new RelationshipSnapshot(
        new Relationship(
            "r-invoices-subscription", "subscriptions", "invoices",
            "fk_invoices_subscription",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY,
            null, null),
        List.of(
            new RelationshipColumn("rc-sub-code", "r-invoices-subscription",
                "subscription_code", "invoice_subscription_code", 1),
            new RelationshipColumn("rc-sub-tenant", "r-invoices-subscription",
                "subscription_tenant", "invoice_tenant", 0)));
    TableSnapshot invoices = new TableSnapshot(
        table("invoices", "invoices"),
        List.of(
            column("invoice_id", "invoices", "id", "BIGINT", null, 0,
                false),
            column("invoice_tenant", "invoices", "tenant_id", "BIGINT",
                null, 1, false),
            column("invoice_subscription_code", "invoices",
                "subscription_code", "VARCHAR",
                new ColumnTypeArguments(30, null, null), 2, false)),
        List.of(pk("pk-invoices", "invoices", "invoice_id")),
        List.of(relationship),
        List.of());

    String ddl = sut.generate(schema(subscriptions, invoices));

    assertThat(ddl).contains(
        "ALTER TABLE `invoices` ADD CONSTRAINT `fk_invoices_subscription` FOREIGN KEY (`tenant_id`, `subscription_code`) REFERENCES `subscriptions` (`tenant_id`, `code`);");
  }

  @Test
  @DisplayName("circular FK는 모든 CREATE TABLE 이후 FK ALTER TABLE을 생성한다")
  void generatesCircularForeignKeysAfterAllCreateTables() {
    RelationshipSnapshot alphaToBeta = new RelationshipSnapshot(
        new Relationship(
            "r-alpha-beta", "beta", "alpha", "fk_alpha_beta",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE,
            null, null),
        List.of(new RelationshipColumn("rc-alpha-beta", "r-alpha-beta",
            "beta_id", "alpha_beta_id", 0)));
    TableSnapshot alpha = new TableSnapshot(
        table("alpha", "alpha"),
        List.of(
            column("alpha_id", "alpha", "id", "BIGINT", null, 0, false),
            column("alpha_beta_id", "alpha", "beta_id", "BIGINT", null, 1,
                false)),
        List.of(pk("pk-alpha", "alpha", "alpha_id")),
        List.of(alphaToBeta),
        List.of());
    RelationshipSnapshot betaToAlpha = new RelationshipSnapshot(
        new Relationship(
            "r-beta-alpha", "alpha", "beta", "fk_beta_alpha",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE,
            null, null),
        List.of(new RelationshipColumn("rc-beta-alpha", "r-beta-alpha",
            "alpha_id", "beta_alpha_id", 0)));
    TableSnapshot beta = new TableSnapshot(
        table("beta", "beta"),
        List.of(
            column("beta_id", "beta", "id", "BIGINT", null, 0, false),
            column("beta_alpha_id", "beta", "alpha_id", "BIGINT", null, 1,
                false)),
        List.of(pk("pk-beta", "beta", "beta_id")),
        List.of(betaToAlpha),
        List.of());

    String ddl = sut.generate(schema(alpha, beta));

    int firstAlter = ddl.indexOf("ALTER TABLE");
    assertThat(ddl.indexOf("CREATE TABLE `alpha`")).isLessThan(firstAlter);
    assertThat(ddl.indexOf("CREATE TABLE `beta`")).isLessThan(firstAlter);
    assertThat(ddl).contains(
        "ALTER TABLE `alpha` ADD CONSTRAINT `fk_alpha_beta` FOREIGN KEY (`beta_id`) REFERENCES `beta` (`id`);",
        "ALTER TABLE `beta` ADD CONSTRAINT `fk_beta_alpha` FOREIGN KEY (`alpha_id`) REFERENCES `alpha` (`id`);");
  }

  @Test
  @DisplayName("지원하지 않는 vendor이면 예외가 발생한다")
  void throwsForUnsupportedVendor() {
    SchemaExportSnapshot snapshot = new SchemaExportSnapshot(
        new SchemaSnapshot("schema-1", "postgresql", "app", null, null),
        List.of(userTable()));

    assertThatThrownBy(() -> sut.generate(snapshot))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.UNSUPPORTED_VENDOR));
  }

  @Test
  @DisplayName("AUTO_INCREMENT 컬럼이 인덱싱되지 않으면 예외가 발생한다")
  void throwsWhenAutoIncrementColumnIsNotIndexed() {
    TableSnapshot table = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "id", "BIGINT", null, 0, true)),
        List.of(),
        List.of(),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(table)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("AUTO_INCREMENT 컬럼이 key의 첫 번째 컬럼이 아니면 예외가 발생한다")
  void throwsWhenAutoIncrementColumnIsNotLeftmostInAnyKey() {
    TableSnapshot table = new TableSnapshot(
        table("orders", "orders"),
        List.of(
            column("tenant_id", "orders", "tenant_id", "BIGINT", null, 0,
                false),
            column("order_id", "orders", "id", "BIGINT", null, 1, true)),
        List.of(),
        List.of(),
        List.of(new IndexSnapshot(
            new Index("idx-orders-tenant-id", "orders",
                "idx_orders_tenant_id", IndexType.BTREE),
            List.of(
                new IndexColumn("ic-orders-tenant", "idx-orders-tenant-id",
                    "tenant_id", 0, SortDirection.ASC),
                new IndexColumn("ic-orders-id", "idx-orders-tenant-id",
                    "order_id", 1, SortDirection.ASC)))));

    assertThatThrownBy(() -> sut.generate(schema(table)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("statement separator가 있는 CHECK expression은 거부한다")
  void throwsWhenCheckExpressionContainsStatementSeparator() {
    ConstraintSnapshot check = constraint(
        "ck1", "t1", "ck_bad", ConstraintKind.CHECK,
        "value > 0; DROP TABLE users", null);
    TableSnapshot table = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "value", "INT", null, 0, false)),
        List.of(check),
        List.of(),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(table)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("InnoDB가 거부하는 FK SET DEFAULT action이면 예외가 발생한다")
  void throwsWhenReferentialActionIsRejectedByInnoDb() {
    RelationshipSnapshot relationship = new RelationshipSnapshot(
        new Relationship(
            "r1", "users", "orders", "fk_orders_user",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY,
            "SET DEFAULT", null),
        List.of(new RelationshipColumn("rc1", "r1", "u_id", "o_user_id", 0)));
    TableSnapshot orders = new TableSnapshot(
        table("orders", "orders"),
        List.of(
            column("o_id", "orders", "id", "BIGINT", null, 0, false),
            column("o_user_id", "orders", "user_id", "BIGINT", null, 1,
                false)),
        List.of(pk("pk-orders", "orders", "o_id")),
        List.of(relationship),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(userTable(), orders)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("InnoDB에서 지원하지 않는 HASH index type이면 예외가 발생한다")
  void throwsWhenHashIndexIsUsedWithInnoDb() {
    TableSnapshot table = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "id", "BIGINT", null, 0, false)),
        List.of(),
        List.of(),
        List.of(new IndexSnapshot(
            new Index("idx1", "t1", "idx_hash", IndexType.HASH),
            List.of(new IndexColumn("ic1", "idx1", "c1", 0,
                SortDirection.ASC)))));

    assertThatThrownBy(() -> sut.generate(schema(table)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("OTHER index type이면 예외가 발생한다")
  void throwsWhenIndexTypeIsOther() {
    TableSnapshot table = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "name", "VARCHAR",
            new ColumnTypeArguments(50, null, null), 0, false)),
        List.of(),
        List.of(),
        List.of(new IndexSnapshot(
            new Index("idx1", "t1", "idx_name", IndexType.OTHER),
            List.of(new IndexColumn("ic1", "idx1", "c1", 0,
                SortDirection.ASC)))));

    assertThatThrownBy(() -> sut.generate(schema(table)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("문자열 계열이 아닌 컬럼에 charset/collation이 있으면 예외가 발생한다")
  void throwsWhenCharsetIsUsedForNonCharacterColumn() {
    TableSnapshot table = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "id", "BIGINT", null, 0, false,
            "utf8mb4", null, null)),
        List.of(),
        List.of(),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(table)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("ENUM/SET type argument가 MySQL 한계를 벗어나면 예외가 발생한다")
  void throwsWhenEnumOrSetArgumentsAreInvalid() {
    TableSnapshot enumWithoutValues = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "status", "ENUM", null, 0, false)),
        List.of(),
        List.of(),
        List.of());
    TableSnapshot setWithTooManyValues = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "flags", "SET",
            new ColumnTypeArguments(null, null, null,
                IntStream.range(0, 65)
                    .mapToObj(index -> "v" + index)
                    .toList()),
            0, false)),
        List.of(),
        List.of(),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(enumWithoutValues)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
    assertThatThrownBy(() -> sut.generate(schema(setWithTooManyValues)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("MySQL type argument matrix에 맞지 않는 snapshot이면 예외가 발생한다")
  void throwsWhenTypeArgumentsDoNotMatchMysqlTypeSyntax() {
    TableSnapshot varcharWithoutLength = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "name", "VARCHAR", null, 0, false)),
        List.of(),
        List.of(),
        List.of());
    TableSnapshot dateWithLength = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "created_on", "DATE",
            new ColumnTypeArguments(10, null, null), 0, false)),
        List.of(),
        List.of(),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(varcharWithoutLength)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
    assertThatThrownBy(() -> sut.generate(schema(dateWithLength)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("SPATIAL index가 단일 NOT NULL spatial column 규칙을 어기면 예외가 발생한다")
  void throwsWhenSpatialIndexViolatesMysqlRules() {
    TableSnapshot table = new TableSnapshot(
        table("t1", "places"),
        List.of(column("c1", "t1", "location", "POINT", null, 0, false)),
        List.of(),
        List.of(),
        List.of(new IndexSnapshot(
            new Index("idx1", "t1", "idx_places_location",
                IndexType.SPATIAL),
            List.of(new IndexColumn("ic1", "idx1", "c1", 0,
                null)))));

    assertThatThrownBy(() -> sut.generate(schema(table)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("FULLTEXT와 SPATIAL index는 sort direction이 있어도 MySQL DDL에 출력하지 않는다")
  void omitsSortDirectionForFulltextAndSpatialIndexes() {
    TableSnapshot table = new TableSnapshot(
        table("places", "places"),
        List.of(
            column("place_id", "places", "id", "BIGINT", null, 0, false),
            column("place_description", "places", "description", "TEXT",
                null, 1, false),
            column("place_location", "places", "location", "POINT", null,
                2, false)),
        List.of(
            pk("pk-places", "places", "place_id"),
            notNull("nn-places-location", "places", "place_location")),
        List.of(),
        List.of(
            new IndexSnapshot(
                new Index("ft-places-description", "places",
                    "ft_places_description", IndexType.FULLTEXT),
                List.of(new IndexColumn("ic-places-description",
                    "ft-places-description", "place_description", 0,
                    SortDirection.ASC))),
            new IndexSnapshot(
                new Index("sp-places-location", "places",
                    "sp_places_location", IndexType.SPATIAL),
                List.of(new IndexColumn("ic-places-location",
                    "sp-places-location", "place_location", 0,
                    SortDirection.DESC)))));

    String ddl = sut.generate(schema(table));

    assertThat(ddl).contains(
        "ALTER TABLE `places` ADD FULLTEXT INDEX `ft_places_description` (`description`);",
        "ALTER TABLE `places` ADD SPATIAL INDEX `sp_places_location` (`location`);");
    assertThat(ddl).doesNotContain(
        "`description` ASC",
        "`location` DESC");
  }

  @Test
  @DisplayName("FK 참조 대상 컬럼이 FULLTEXT index로만 인덱싱되어 있으면 예외가 발생한다")
  void throwsWhenReferencedForeignKeyColumnsOnlyHaveFulltextIndex() {
    TableSnapshot users = new TableSnapshot(
        table("users", "users"),
        List.of(column("u_email", "users", "email", "VARCHAR",
            new ColumnTypeArguments(255, null, null), 0, false)),
        List.of(),
        List.of(),
        List.of(new IndexSnapshot(
            new Index("idx-users-email", "users", "ft_users_email",
                IndexType.FULLTEXT),
            List.of(new IndexColumn("ic-users-email", "idx-users-email",
                "u_email", 0, null)))));
    RelationshipSnapshot relationship = new RelationshipSnapshot(
        new Relationship(
            "r1", "users", "orders", "fk_orders_user_email",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY,
            null, null),
        List.of(new RelationshipColumn("rc1", "r1", "u_email",
            "o_user_email", 0)));
    TableSnapshot orders = new TableSnapshot(
        table("orders", "orders"),
        List.of(column("o_user_email", "orders", "user_email", "VARCHAR",
            new ColumnTypeArguments(255, null, null), 0, false)),
        List.of(),
        List.of(relationship),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(users, orders)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("FK 컬럼 타입이 MySQL 호환 규칙에 맞지 않으면 예외가 발생한다")
  void throwsWhenForeignKeyColumnTypesAreIncompatible() {
    RelationshipSnapshot relationship = new RelationshipSnapshot(
        new Relationship(
            "r1", "users", "orders", "fk_orders_user",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY,
            null, null),
        List.of(new RelationshipColumn("rc1", "r1", "u_id", "o_user_id", 0)));
    TableSnapshot orders = new TableSnapshot(
        table("orders", "orders"),
        List.of(
            column("o_id", "orders", "id", "BIGINT", null, 0, false),
            column("o_user_id", "orders", "user_id", "VARCHAR",
                new ColumnTypeArguments(255, null, null), 1, false)),
        List.of(pk("pk-orders", "orders", "o_id")),
        List.of(relationship),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(userTable(), orders)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("FK SET NULL action이 NOT NULL 컬럼을 대상으로 하면 예외가 발생한다")
  void throwsWhenSetNullTargetsNotNullColumn() {
    RelationshipSnapshot relationship = new RelationshipSnapshot(
        new Relationship(
            "r1", "users", "orders", "fk_orders_user",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY,
            "SET NULL", null),
        List.of(new RelationshipColumn("rc1", "r1", "u_id", "o_user_id", 0)));
    TableSnapshot orders = new TableSnapshot(
        table("orders", "orders"),
        List.of(
            column("o_id", "orders", "id", "BIGINT", null, 0, false),
            column("o_user_id", "orders", "user_id", "BIGINT", null, 1,
                false)),
        List.of(
            pk("pk-orders", "orders", "o_id"),
            notNull("nn-orders-user", "orders", "o_user_id")),
        List.of(relationship),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(userTable(), orders)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("FK 참조 대상 컬럼이 인덱싱되어 있지 않으면 예외가 발생한다")
  void throwsWhenReferencedForeignKeyColumnsAreNotIndexed() {
    RelationshipSnapshot relationship = new RelationshipSnapshot(
        new Relationship(
            "r1", "users", "orders", "fk_orders_user",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY,
            null, null),
        List.of(new RelationshipColumn("rc1", "r1", "u_id", "o_user_id", 0)));
    TableSnapshot usersWithoutKey = new TableSnapshot(
        table("users", "users"),
        List.of(column("u_id", "users", "id", "BIGINT", null, 0, false)),
        List.of(),
        List.of(),
        List.of());
    TableSnapshot orders = new TableSnapshot(
        table("orders", "orders"),
        List.of(
            column("o_id", "orders", "id", "BIGINT", null, 0, false),
            column("o_user_id", "orders", "user_id", "BIGINT", null, 1,
                false)),
        List.of(pk("pk-orders", "orders", "o_id")),
        List.of(relationship),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(usersWithoutKey, orders)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.INVALID_VALUE));
  }

  @Test
  @DisplayName("참조 컬럼이 snapshot에 없으면 예외가 발생한다")
  void throwsWhenReferencedColumnIsMissing() {
    ConstraintSnapshot unique = new ConstraintSnapshot(
        new Constraint("uk1", "t1", "uk_missing", ConstraintKind.UNIQUE,
            null, null),
        List.of(new ConstraintColumn("cc1", "uk1", "missing", 0)));
    TableSnapshot table = new TableSnapshot(
        table("t1", "users"),
        List.of(column("c1", "t1", "name", "VARCHAR",
            new ColumnTypeArguments(50, null, null), 0, false)),
        List.of(unique),
        List.of(),
        List.of());

    assertThatThrownBy(() -> sut.generate(schema(table)))
        .isInstanceOf(DomainException.class)
        .matches(DomainException.hasErrorCode(DdlErrorCode.COLUMN_NOT_FOUND));
  }

  private SchemaExportSnapshot schema(TableSnapshot... tables) {
    return new SchemaExportSnapshot(
        new SchemaSnapshot("schema-1", "mysql", "app`schema",
            "utf8mb4", "utf8mb4_unicode_ci"),
        List.of(tables));
  }

  private TableSnapshot userTable() {
    return new TableSnapshot(
        table("users", "users"),
        List.of(
            column("u_id", "users", "id", "BIGINT", null, 0, true),
            column("u_email", "users", "email", "VARCHAR",
                new ColumnTypeArguments(255, null, null), 1, false)),
        List.of(
            pk("pk-users", "users", "u_id"),
            notNull("nn-users-email", "users", "u_email"),
            unique("uk-users-email", "users", "uk_users_email", "u_email")),
        List.of(),
        List.of());
  }

  private TableSnapshot orderTable() {
    return new TableSnapshot(
        table("orders", "orders"),
        List.of(
            column("o_user_id", "orders", "user_id", "BIGINT", null, 1,
                false),
            column("o_id", "orders", "id", "BIGINT", null, 0, true),
            column("o_status", "orders", "status", "ENUM",
                new ColumnTypeArguments(null, null, null,
                    List.of("READY", "it\\'s")),
                2, false, "utf8mb4", "utf8mb4_unicode_ci",
                "order's status"),
            column("o_total", "orders", "total", "DECIMAL",
                new ColumnTypeArguments(null, 10, 2), 3, false)),
        List.of(
            pk("pk-orders", "orders", "o_id"),
            notNull("nn-orders-user", "orders", "o_user_id"),
            defaultConstraint("df-orders-status", "orders", "o_status",
                "'READY'"),
            constraint("ck-orders-total", "orders", "ck_order_total",
                ConstraintKind.CHECK, "total >= 0", null)),
        List.of(new RelationshipSnapshot(
            new Relationship(
                "r1", "users", "orders", "fk_orders_user",
                RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY,
                "cascade", "no action"),
            List.of(new RelationshipColumn(
                "rc1", "r1", "u_id", "o_user_id", 0)))),
        List.of(new IndexSnapshot(
            new Index("idx-orders-user", "orders", "idx_orders_user",
                IndexType.BTREE),
            List.of(new IndexColumn(
                "ic1", "idx-orders-user", "o_user_id", 0,
                SortDirection.ASC)))));
  }

  private Table table(String id, String name) {
    return new Table(id, "schema-1", name, "utf8mb4",
        "utf8mb4_unicode_ci");
  }

  private Column column(String id, String tableId, String name,
      String dataType, ColumnTypeArguments args, int seqNo,
      boolean autoIncrement) {
    return column(id, tableId, name, dataType, args, seqNo, autoIncrement,
        null, null, null);
  }

  private Column column(String id, String tableId, String name,
      String dataType, ColumnTypeArguments args, int seqNo,
      boolean autoIncrement, String charset, String collation,
      String comment) {
    return new Column(id, tableId, name, dataType, args, seqNo,
        autoIncrement, charset, collation, comment);
  }

  private ConstraintSnapshot pk(String id, String tableId,
      String... columnIds) {
    return new ConstraintSnapshot(
        new Constraint(id, tableId, "PRIMARY", ConstraintKind.PRIMARY_KEY,
            null, null),
        IntStream.range(0, columnIds.length)
            .mapToObj(index -> new ConstraintColumn(
                id + "-col-" + index, id, columnIds[index], index))
            .toList());
  }

  private ConstraintSnapshot notNull(String id, String tableId,
      String columnId) {
    return new ConstraintSnapshot(
        new Constraint(id, tableId, id, ConstraintKind.NOT_NULL, null, null),
        List.of(new ConstraintColumn(id + "-col", id, columnId, 0)));
  }

  private ConstraintSnapshot unique(String id, String tableId, String name,
      String columnId) {
    return new ConstraintSnapshot(
        new Constraint(id, tableId, name, ConstraintKind.UNIQUE, null, null),
        List.of(new ConstraintColumn(id + "-col", id, columnId, 0)));
  }

  private ConstraintSnapshot defaultConstraint(String id, String tableId,
      String columnId, String defaultExpr) {
    return new ConstraintSnapshot(
        new Constraint(id, tableId, id, ConstraintKind.DEFAULT, null,
            defaultExpr),
        List.of(new ConstraintColumn(id + "-col", id, columnId, 0)));
  }

  private ConstraintSnapshot constraint(String id, String tableId, String name,
      ConstraintKind kind, String checkExpr, String defaultExpr) {
    return new ConstraintSnapshot(
        new Constraint(id, tableId, name, kind, checkExpr, defaultExpr),
        List.of());
  }

}
