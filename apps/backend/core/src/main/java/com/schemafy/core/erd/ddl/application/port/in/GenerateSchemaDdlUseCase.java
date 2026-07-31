package com.schemafy.core.erd.ddl.application.port.in;

import reactor.core.publisher.Mono;

public interface GenerateSchemaDdlUseCase {

  Mono<String> generateSchemaDdl(GenerateSchemaDdlCommand command);

}
