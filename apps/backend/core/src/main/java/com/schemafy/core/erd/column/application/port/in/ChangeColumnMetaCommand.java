package com.schemafy.core.erd.column.application.port.in;

import com.schemafy.core.common.PatchField;

public record ChangeColumnMetaCommand(
    String columnId,
    PatchField<Boolean> autoIncrement,
    PatchField<String> charset,
    PatchField<String> collation,
    PatchField<String> comment) {
}
