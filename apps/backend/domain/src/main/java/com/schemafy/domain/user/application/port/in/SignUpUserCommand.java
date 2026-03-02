package com.schemafy.domain.user.application.port.in;

public record SignUpUserCommand(String email, String name, String password) {
}

