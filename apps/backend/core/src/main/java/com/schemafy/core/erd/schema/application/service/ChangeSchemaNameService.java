package com.schemafy.core.erd.schema.application.service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameUseCase;
import com.schemafy.core.erd.schema.application.port.out.ChangeSchemaNamePort;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.domain.validator.IdentifierValidator;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.SCHEMA;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = SCHEMA, id = "schemaId"))
public class ChangeSchemaNameService implements ChangeSchemaNameUseCase {

  private final ChangeSchemaNamePort changeSchemaNamePort;
  private final SchemaExistsPort schemaExistsPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final IdentifierCapabilityResolver identifierCapabilityResolver;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeSchemaName(ChangeSchemaNameCommand command) {
    return getSchemaByIdPort.findSchemaById(command.schemaId())
        .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: " + command
            .schemaId())))
        .flatMap(schema -> identifierCapabilityResolver.resolve(SCHEMA, command.schemaId())
            .flatMap(identifiers -> {
              IdentifierValidator.validateLength(
                  identifiers,
                  command.newName(),
                  SchemaErrorCode.INVALID_VALUE,
                  "Schema name");
              if (Objects.equals(schema.name(), command.newName())) {
                return Mono.just(MutationResult.<Void>noop(null));
              }
              return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_SCHEMA_NAME, command,
                  () -> getSchemaByIdPort.findSchemaById(command.schemaId())
                      .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: "
                          + command.schemaId())))
                      .flatMap(lockedSchema -> {
                        if (Objects.equals(lockedSchema.name(), command.newName())) {
                          return Mono.just(MutationResult.<Void>noop(null));
                        }
                        return schemaExistsPort
                            .existsActiveByProjectIdAndName(lockedSchema.projectId(), command.newName())
                            .flatMap(exists -> {
                              if (exists) {
                                return Mono.error(new DomainException(SchemaErrorCode.NAME_DUPLICATE,
                                    "A schema with the name '" + command.newName()
                                        + "' already exists in the project."));
                              }

                              return changeSchemaNamePort.changeSchemaName(command.schemaId(), command.newName())
                                  .thenReturn(MutationResult.empty(null));
                            });
                      }));
            }));
  }

}
