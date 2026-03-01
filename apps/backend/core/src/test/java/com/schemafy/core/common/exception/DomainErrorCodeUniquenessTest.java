package com.schemafy.core.common.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.schemafy.core.erd.exception.MemoErrorCode;
import com.schemafy.core.project.exception.ProjectErrorCode;
import com.schemafy.core.project.exception.ShareLinkErrorCode;
import com.schemafy.core.project.exception.WorkspaceErrorCode;
import com.schemafy.core.user.exception.UserErrorCode;
import com.schemafy.domain.common.exception.DomainErrorCode;
import com.schemafy.domain.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.domain.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.domain.erd.table.domain.exception.TableErrorCode;
import com.schemafy.domain.erd.vendor.domain.exception.VendorErrorCode;
import com.schemafy.domain.ulid.exception.UlidErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

class DomainErrorCodeUniquenessTest {

  private static final List<Class<? extends Enum<?>>> ERROR_CODE_ENUMS = List.of(
      CommonErrorCode.class,
      AuthErrorCode.class,
      ValidationErrorCode.class,
      HmacErrorCode.class,
      UserErrorCode.class,
      WorkspaceErrorCode.class,
      ProjectErrorCode.class,
      ShareLinkErrorCode.class,
      MemoErrorCode.class,
      SchemaErrorCode.class,
      TableErrorCode.class,
      ColumnErrorCode.class,
      ConstraintErrorCode.class,
      IndexErrorCode.class,
      RelationshipErrorCode.class,
      VendorErrorCode.class,
      UlidErrorCode.class);

  @Test
  void codesMustBeGloballyUnique() {
    Map<String, Class<? extends Enum<?>>> seen = new HashMap<>();

    for (Class<? extends Enum<?>> enumClass : ERROR_CODE_ENUMS) {
      for (Enum<?> constant : enumClass.getEnumConstants()) {
        DomainErrorCode errorCode = (DomainErrorCode) constant;
        String code = errorCode.code();

        Class<? extends Enum<?>> previous = seen.putIfAbsent(code,
            enumClass);
        assertThat(previous)
            .as("Duplicate error code '%s' between %s and %s", code,
                previous, enumClass)
            .isNull();
      }
    }
  }

}
