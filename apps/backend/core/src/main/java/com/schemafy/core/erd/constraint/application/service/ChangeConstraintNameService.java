package com.schemafy.core.erd.constraint.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.core.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.constraint.domain.validator.ConstraintValidator;
import com.schemafy.core.erd.operation.application.inverse.ChangeConstraintNameInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.domain.validator.IdentifierValidator;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.CONSTRAINT;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = CONSTRAINT, id = "constraintId"))
public class ChangeConstraintNameService implements ChangeConstraintNameUseCase {

  private final ChangeConstraintNamePort changeConstraintNamePort;
  private final ConstraintExistsPort constraintExistsPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final IdentifierCapabilityResolver identifierCapabilityResolver;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeConstraintName(ChangeConstraintNameCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.newName());
      ConstraintValidator.validateName(normalizedName);
      return getConstraintByIdPort.findConstraintById(command.constraintId())
          .switchIfEmpty(Mono.error(new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found")))
          .flatMap(constraint -> {
            if (normalizedName.equals(constraint.name())) {
              return Mono.just(MutationResult.<Void>noop(null, constraint.tableId()));
            }
            return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_CONSTRAINT_NAME, command,
                () -> getConstraintByIdPort.findConstraintById(command.constraintId())
                    .switchIfEmpty(Mono.error(new DomainException(
                        ConstraintErrorCode.NOT_FOUND,
                        "Constraint not found")))
                    .flatMap(lockedConstraint -> {
                      if (normalizedName.equals(lockedConstraint.name())) {
                        return Mono.just(MutationResult.<Void>noop(null, lockedConstraint.tableId()));
                      }
                      return identifierCapabilityResolver.resolve(CONSTRAINT, lockedConstraint.id())
                          .flatMap(identifierCapabilities -> {
                            IdentifierValidator.validateLength(
                                identifierCapabilities,
                                normalizedName,
                                ConstraintErrorCode.NAME_INVALID,
                                "Constraint name");
                            return getTableByIdPort.findTableById(lockedConstraint.tableId())
                                .switchIfEmpty(Mono.error(
                                    new DomainException(TableErrorCode.NOT_FOUND, "Table not found")))
                                .flatMap(table -> constraintExistsPort.existsBySchemaIdAndNameExcludingId(
                                    table.schemaId(),
                                    normalizedName,
                                    lockedConstraint.id())
                                    .flatMap(exists -> {
                                      if (exists) {
                                        return Mono.error(new DomainException(ConstraintErrorCode.NAME_DUPLICATE,
                                            "Constraint name '%s' already exists in schema".formatted(normalizedName)));
                                      }
                                      return changeConstraintNamePort
                                          .changeConstraintName(lockedConstraint.id(), normalizedName)
                                          .thenReturn(MutationResult.<Void>of(null, lockedConstraint.tableId())
                                              .withInverse(new ChangeConstraintNameInverse(
                                                  lockedConstraint.id(),
                                                  lockedConstraint.name())));
                                    }));
                          });
                    }));
          });
    });
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
