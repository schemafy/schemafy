package com.schemafy.core.user.application.port.in;

public record SignUpUserCommand(String email, String name, String password) {

  public SignUpUserCommand withEmail(String email) {
    return new SignUpUserCommand(email, name, password);
  }

}
