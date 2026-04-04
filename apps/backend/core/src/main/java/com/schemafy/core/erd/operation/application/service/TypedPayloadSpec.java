package com.schemafy.core.erd.operation.application.service;

interface TypedPayloadSpec<T> {

  Class<T> payloadType();

  default T requirePayload(Object payload, String unsupportedPayloadPrefix) {
    if (payloadType().isInstance(payload)) {
      return payloadType().cast(payload);
    }
    throw new IllegalArgumentException(unsupportedPayloadPrefix + payloadType().getSimpleName());
  }

}
