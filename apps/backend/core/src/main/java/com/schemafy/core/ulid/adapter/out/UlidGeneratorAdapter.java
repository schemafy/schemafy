package com.schemafy.core.ulid.adapter.out;

import org.springframework.stereotype.Component;

import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.ulid.application.service.UlidGenerator;

@Component
class UlidGeneratorAdapter implements UlidGeneratorPort {

  @Override
  public String generate() {
    return UlidGenerator.generate();
  }

}
