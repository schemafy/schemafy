package com.schemafy.core.erd.ddl.application.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlCommand;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlUseCase;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.ddl.domain.DdlGenerator;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot;
import com.schemafy.core.erd.ddl.domain.exception.DdlErrorCode;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.index.domain.validator.IndexValidator;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.erd.vendor.domain.validator.IdentifierValidator;

import reactor.core.publisher.Mono;

@Service
public class GenerateSchemaDdlService implements GenerateSchemaDdlUseCase {

  private final Map<DdlExportVendor, DdlGenerator> ddlGenerators;

  public GenerateSchemaDdlService(List<DdlGenerator> ddlGenerators) {
    this.ddlGenerators = ddlGenerators.stream()
        .collect(Collectors.toUnmodifiableMap(
            DdlGenerator::exportVendor,
            Function.identity(),
            (left, right) -> {
              throw new IllegalStateException(
                  "Duplicate DDL generator for target DB vendor: "
                      + left.exportVendor().value());
            }));
  }

  @Override
  public Mono<String> generateSchemaDdl(GenerateSchemaDdlCommand command) {
    return Mono.fromSupplier(() -> {
      DdlGenerator generator = ddlGenerators.get(command.targetDbVendor());
      if (generator == null) {
        throw new DomainException(DdlErrorCode.UNSUPPORTED_VENDOR,
            "Unsupported DDL export target DB vendor: "
                + command.targetDbVendor().value());
      }
      validateIndexCapabilities(command.snapshot(), command.indexCapabilities());
      validateIdentifierCapabilities(
          command.snapshot(),
          command.identifierCapabilities());
      return generator.generate(command.snapshot());
    });
  }

  private static void validateIndexCapabilities(
      DdlSchemaSnapshot snapshot,
      IndexCapabilities indexCapabilities) {
    snapshot.tables().stream()
        .flatMap(table -> table.indexes().stream())
        .map(DdlSchemaSnapshot.IndexSnapshot::index)
        .forEach(index -> IndexValidator.validateType(indexCapabilities, index.type()));
  }

  private static void validateIdentifierCapabilities(
      DdlSchemaSnapshot snapshot,
      IdentifierCapabilities identifierCapabilities) {
    if (snapshot == null || snapshot.schema() == null) {
      return;
    }

    validateIdentifierLength(
        identifierCapabilities,
        snapshot.schema().name(),
        "Schema name");
    for (DdlSchemaSnapshot.TableSnapshot tableSnapshot : snapshot.tables()) {
      if (tableSnapshot == null) {
        continue;
      }
      if (tableSnapshot.table() != null) {
        validateIdentifierLength(
            identifierCapabilities,
            tableSnapshot.table().name(),
            "Table name");
      }
      tableSnapshot.columns().stream()
          .filter(Objects::nonNull)
          .forEach(column -> validateIdentifierLength(
              identifierCapabilities,
              column.name(),
              "Column name"));
      tableSnapshot.constraints().stream()
          .filter(Objects::nonNull)
          .map(DdlSchemaSnapshot.ConstraintSnapshot::constraint)
          .filter(Objects::nonNull)
          .forEach(constraint -> validateIdentifierLength(
              identifierCapabilities,
              constraint.name(),
              "Constraint name"));
      tableSnapshot.indexes().stream()
          .filter(Objects::nonNull)
          .map(DdlSchemaSnapshot.IndexSnapshot::index)
          .filter(Objects::nonNull)
          .forEach(index -> validateIdentifierLength(
              identifierCapabilities,
              index.name(),
              "Index name"));
      tableSnapshot.relationships().stream()
          .filter(Objects::nonNull)
          .map(DdlSchemaSnapshot.RelationshipSnapshot::relationship)
          .filter(Objects::nonNull)
          .forEach(relationship -> validateIdentifierLength(
              identifierCapabilities,
              relationship.name(),
              "Relationship name"));
    }
  }

  private static void validateIdentifierLength(
      IdentifierCapabilities identifierCapabilities,
      String name,
      String subject) {
    IdentifierValidator.validateLength(
        identifierCapabilities,
        name,
        DdlErrorCode.INVALID_VALUE,
        subject);
  }

}
