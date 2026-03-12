package com.schemafy.core.user.application.port.in;

public record SignUpUserCommand(String email, String name, String password) {
}
