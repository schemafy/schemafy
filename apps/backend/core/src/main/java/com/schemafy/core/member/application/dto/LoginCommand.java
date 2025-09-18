package com.schemafy.core.member.application.dto;

public record LoginCommand(
        String email,
        String password
) {
}
