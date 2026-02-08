package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.PatchField;

public record ChangeTableMetaCommand(
    String tableId,
    PatchField<String> charset,
    PatchField<String> collation) {
}
