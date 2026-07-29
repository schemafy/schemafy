package com.schemafy.core.erd.mermaid.application.port.in;

import reactor.core.publisher.Mono;

public interface GenerateSchemaMermaidUseCase {

  Mono<String> generateSchemaMermaid(GenerateSchemaMermaidCommand command);

}
