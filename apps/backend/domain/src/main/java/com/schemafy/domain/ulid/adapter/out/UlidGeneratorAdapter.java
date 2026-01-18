package com.schemafy.domain.ulid.adapter.out;

import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.domain.ulid.application.service.UlidGenerator;

public class UlidGeneratorAdapter implements UlidGeneratorPort {

  @Override
  public String generate() {
    return UlidGenerator.generate();
  }

}
