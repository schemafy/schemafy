package com.schemafy.domain.erd.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.CreateRelationshipColumnCommand;
import com.schemafy.domain.erd.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.application.port.in.CreateRelationshipResult;
import com.schemafy.domain.erd.application.port.in.CreateRelationshipUseCase;
import com.schemafy.domain.erd.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.domain.erd.application.port.out.CreateRelationshipPort;
import com.schemafy.domain.erd.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.domain.Column;
import com.schemafy.domain.erd.domain.Relationship;
import com.schemafy.domain.erd.domain.RelationshipColumn;
import com.schemafy.domain.erd.domain.Table;
import com.schemafy.domain.erd.domain.exception.RelationshipNameDuplicateException;
import com.schemafy.domain.erd.domain.exception.RelationshipTargetTableNotExistException;
import com.schemafy.domain.erd.domain.validator.RelationshipValidator;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CreateRelationshipService implements CreateRelationshipUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateRelationshipPort createRelationshipPort;
  private final CreateRelationshipColumnPort createRelationshipColumnPort;
  private final RelationshipExistsPort relationshipExistsPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;

  public CreateRelationshipService(
      UlidGeneratorPort ulidGeneratorPort,
      CreateRelationshipPort createRelationshipPort,
      CreateRelationshipColumnPort createRelationshipColumnPort,
      RelationshipExistsPort relationshipExistsPort,
      GetTableByIdPort getTableByIdPort,
      GetColumnsByTableIdPort getColumnsByTableIdPort,
      GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort) {
    this.ulidGeneratorPort = ulidGeneratorPort;
    this.createRelationshipPort = createRelationshipPort;
    this.createRelationshipColumnPort = createRelationshipColumnPort;
    this.relationshipExistsPort = relationshipExistsPort;
    this.getTableByIdPort = getTableByIdPort;
    this.getColumnsByTableIdPort = getColumnsByTableIdPort;
    this.getRelationshipsBySchemaIdPort = getRelationshipsBySchemaIdPort;
  }

  @Override
  public Mono<CreateRelationshipResult> createRelationship(CreateRelationshipCommand command) {
    String normalizedName = normalizeName(command.name());
    String normalizedExtra = normalizeOptional(command.extra());
    List<CreateRelationshipColumnCommand> columnCommands = normalizeColumns(command.columns());

    RelationshipValidator.validateName(normalizedName);
    RelationshipValidator.validateColumnsNotEmpty(toColumns(columnCommands), normalizedName);

    return getTableByIdPort.findTableById(command.fkTableId())
        .switchIfEmpty(Mono.error(new RelationshipTargetTableNotExistException(
            "Relationship '%s' fk table not found".formatted(normalizedName))))
        .flatMap(fkTable -> getTableByIdPort.findTableById(command.pkTableId())
            .switchIfEmpty(Mono.error(new RelationshipTargetTableNotExistException(
                "Relationship '%s' pk table not found".formatted(normalizedName))))
            .flatMap(pkTable -> validateAndCreate(
                fkTable,
                pkTable,
                command,
                normalizedName,
                normalizedExtra,
                columnCommands)));
  }

  private Mono<CreateRelationshipResult> validateAndCreate(
      Table fkTable,
      Table pkTable,
      CreateRelationshipCommand command,
      String normalizedName,
      String normalizedExtra,
      List<CreateRelationshipColumnCommand> columnCommands) {
    if (!fkTable.schemaId().equals(pkTable.schemaId())) {
      return Mono.error(new RelationshipTargetTableNotExistException(
          "Relationship '%s' tables must belong to the same schema".formatted(normalizedName)));
    }
    if (command.kind() == null || command.cardinality() == null) {
      return Mono.error(new IllegalArgumentException("Relationship kind and cardinality are required"));
    }

    return relationshipExistsPort.existsByFkTableIdAndName(fkTable.id(), normalizedName)
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new RelationshipNameDuplicateException(
                "Relationship name '%s' already exists in table".formatted(normalizedName)));
          }
          return validateColumnsAndCycle(
              fkTable,
              pkTable,
              command,
              normalizedName,
              normalizedExtra,
              columnCommands);
        });
  }

  private Mono<CreateRelationshipResult> validateColumnsAndCycle(
      Table fkTable,
      Table pkTable,
      CreateRelationshipCommand command,
      String normalizedName,
      String normalizedExtra,
      List<CreateRelationshipColumnCommand> columnCommands) {
    List<RelationshipColumn> relationshipColumns = toColumns(columnCommands);
    List<Integer> seqNos = columnCommands.stream()
        .map(CreateRelationshipColumnCommand::seqNo)
        .toList();

    return Mono.zip(
        getColumnsByTableIdPort.findColumnsByTableId(fkTable.id()).defaultIfEmpty(List.of()),
        getColumnsByTableIdPort.findColumnsByTableId(pkTable.id()).defaultIfEmpty(List.of()),
        getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(fkTable.schemaId())
            .defaultIfEmpty(List.of()))
        .flatMap(tuple -> {
          List<Column> fkColumns = tuple.getT1();
          List<Column> pkColumns = tuple.getT2();
          List<Relationship> relationships = tuple.getT3();

          RelationshipValidator.validateSeqNoIntegrity(seqNos);
          RelationshipValidator.validateColumnExistence(
              fkColumns,
              pkColumns,
              relationshipColumns,
              normalizedName);
          RelationshipValidator.validateColumnUniqueness(relationshipColumns, normalizedName);
          if (command.kind() == com.schemafy.domain.erd.domain.type.RelationshipKind.IDENTIFYING) {
            RelationshipValidator.validateIdentifyingCycle(
                relationships,
                null,
                new Relationship(
                    "new",
                    pkTable.id(),
                    fkTable.id(),
                    normalizedName,
                    command.kind(),
                    command.cardinality(),
                    normalizedExtra));
          }

          return persistRelationship(
              fkTable,
              pkTable,
              command,
              normalizedName,
              normalizedExtra,
              columnCommands);
        });
  }

  private Mono<CreateRelationshipResult> persistRelationship(
      Table fkTable,
      Table pkTable,
      CreateRelationshipCommand command,
      String normalizedName,
      String normalizedExtra,
      List<CreateRelationshipColumnCommand> columnCommands) {
    String relationshipId = ulidGeneratorPort.generate();
    Relationship relationship = new Relationship(
        relationshipId,
        pkTable.id(),
        fkTable.id(),
        normalizedName,
        command.kind(),
        command.cardinality(),
        normalizedExtra);

    return createRelationshipPort.createRelationship(relationship)
        .flatMap(savedRelationship -> createRelationshipColumns(
            relationshipId,
            columnCommands)
            .thenReturn(new CreateRelationshipResult(
                savedRelationship.id(),
                savedRelationship.fkTableId(),
                savedRelationship.pkTableId(),
                savedRelationship.name(),
                savedRelationship.kind(),
                savedRelationship.cardinality(),
                savedRelationship.extra())));
  }

  private Mono<Void> createRelationshipColumns(
      String relationshipId,
      List<CreateRelationshipColumnCommand> columnCommands) {
    return Flux.fromIterable(columnCommands)
        .concatMap(command -> createRelationshipColumnPort.createRelationshipColumn(
            new RelationshipColumn(
                ulidGeneratorPort.generate(),
                relationshipId,
                command.pkColumnId(),
                command.fkColumnId(),
                command.seqNo())))
        .then();
  }

  private static List<CreateRelationshipColumnCommand> normalizeColumns(
      List<CreateRelationshipColumnCommand> columns) {
    if (columns == null) {
      return List.of();
    }
    return List.copyOf(columns);
  }

  private static List<RelationshipColumn> toColumns(List<CreateRelationshipColumnCommand> commands) {
    if (commands == null) {
      return List.of();
    }
    return commands.stream()
        .map(command -> new RelationshipColumn(
            null,
            null,
            command.pkColumnId(),
            command.fkColumnId(),
            command.seqNo()))
        .toList();
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
