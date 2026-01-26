package com.schemafy.domain.erd.index.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexNamePort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.IndexExistsPort;
import com.schemafy.domain.erd.index.domain.exception.IndexNameDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.domain.validator.IndexValidator;

import reactor.core.publisher.Mono;

@Service
public class ChangeIndexNameService implements ChangeIndexNameUseCase {

  private final ChangeIndexNamePort changeIndexNamePort;
  private final IndexExistsPort indexExistsPort;
  private final GetIndexByIdPort getIndexByIdPort;

  public ChangeIndexNameService(
      ChangeIndexNamePort changeIndexNamePort,
      IndexExistsPort indexExistsPort,
      GetIndexByIdPort getIndexByIdPort) {
    this.changeIndexNamePort = changeIndexNamePort;
    this.indexExistsPort = indexExistsPort;
    this.getIndexByIdPort = getIndexByIdPort;
  }

  @Override
  public Mono<Void> changeIndexName(ChangeIndexNameCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.newName());
      IndexValidator.validateName(normalizedName);
      return getIndexByIdPort.findIndexById(command.indexId())
          .switchIfEmpty(Mono.error(new IndexNotExistException("Index not found")))
          .flatMap(index -> indexExistsPort.existsByTableIdAndNameExcludingId(
              index.tableId(),
              normalizedName,
              index.id())
              .flatMap(exists -> {
                if (exists) {
                  return Mono.error(new IndexNameDuplicateException(
                      "Index name '%s' already exists in table".formatted(normalizedName)));
                }
                return changeIndexNamePort.changeIndexName(index.id(), normalizedName);
              }));
    });
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
