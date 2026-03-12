package com.schemafy.core.user.application.port.in;

public record LoginUserCommand(String email, String password) {
}
