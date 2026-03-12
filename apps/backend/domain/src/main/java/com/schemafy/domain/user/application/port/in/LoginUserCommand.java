package com.schemafy.domain.user.application.port.in;

public record LoginUserCommand(String email, String password) {
}
