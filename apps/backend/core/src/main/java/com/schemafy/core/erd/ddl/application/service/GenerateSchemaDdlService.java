package com.schemafy.core.erd.ddl.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlCommand;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlUseCase;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.ddl.domain.DdlGenerator;
import com.schemafy.core.erd.ddl.domain.exception.DdlErrorCode;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.index.domain.validator.IndexValidator;

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
      return generator.generate(command.snapshot());
    });
  }

  private static void validateIndexCapabilities(
      SchemaExportSnapshot snapshot,
      IndexCapabilities indexCapabilities) {
    snapshot.tables().stream()
        .flatMap(table -> table.indexes().stream())
        .map(SchemaExportSnapshot.IndexSnapshot::index)
        .forEach(index -> IndexValidator.validateType(indexCapabilities, index.type()));
  }

}
