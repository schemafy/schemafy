package com.schemafy.core.erd.controller.dto.request;

import validation.Validation.CreateTableRequest;

public record CreateTableRequestWithExtra(
        CreateTableRequest request,
        String extra) {
}
