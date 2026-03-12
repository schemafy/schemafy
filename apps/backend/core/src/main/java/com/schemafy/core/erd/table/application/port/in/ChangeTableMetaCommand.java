package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.common.PatchField;

public record ChangeTableMetaCommand(
    String tableId,
    PatchField<String> charset,
    PatchField<String> collation) {
}
