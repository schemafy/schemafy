package com.schemafy.core.common;

import java.util.Objects;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.service.dto.SignUpCommand;

import reactor.core.publisher.Mono;

public class TestFixture {

  public static PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public static Mono<User> createTestUser(String email, String name,
      String password) {
    String defaultEmail = Objects.requireNonNullElse(email,
        "test@example.com");
    String defaultName = Objects.requireNonNullElse(name, "Test User");
    String defaultPassword = Objects.requireNonNullElse(password,
        "encodedPassword");

    SignUpCommand command = new SignUpCommand(defaultEmail, defaultName,
        defaultPassword);
    return User.signUp(command.toUserInfo(), passwordEncoder);
  }

}
