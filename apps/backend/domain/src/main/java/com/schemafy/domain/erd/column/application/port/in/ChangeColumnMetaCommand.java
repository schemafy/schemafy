package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.common.PatchField;

public record ChangeColumnMetaCommand(
    String columnId,
    PatchField<Boolean> autoIncrement,
    PatchField<String> charset,
    PatchField<String> collation,
    PatchField<String> comment) {
}
