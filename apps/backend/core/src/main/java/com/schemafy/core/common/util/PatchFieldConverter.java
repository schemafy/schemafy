package com.schemafy.core.common.util;

import org.openapitools.jackson.nullable.JsonNullable;

import com.schemafy.domain.common.PatchField;

public final class PatchFieldConverter {

  private PatchFieldConverter() {}

  public static <T> PatchField<T> toPatchField(JsonNullable<T> nullable) {
    if (nullable == null || !nullable.isPresent()) {
      return PatchField.absent();
    }
    return PatchField.of(nullable.get());
  }

}
