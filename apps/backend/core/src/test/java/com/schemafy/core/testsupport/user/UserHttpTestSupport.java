package com.schemafy.core.testsupport.user;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.domain.ulid.application.service.UlidGenerator;
import com.schemafy.domain.user.application.port.out.CreateUserAuthProviderPort;
import com.schemafy.domain.user.application.port.out.CreateUserPort;
import com.schemafy.domain.user.application.port.out.FindUserByEmailPort;
import com.schemafy.domain.user.application.port.out.FindUserByIdPort;
import com.schemafy.domain.user.domain.AuthProvider;
import com.schemafy.domain.user.domain.User;
import com.schemafy.domain.user.domain.UserAuthProvider;

import reactor.core.publisher.Mono;

public abstract class UserHttpTestSupport {

  @Autowired
  protected CreateUserPort createUserPort;

  @Autowired
  protected CreateUserAuthProviderPort createUserAuthProviderPort;

  @Autowired
  protected FindUserByEmailPort findUserByEmailPort;

  @Autowired
  protected FindUserByIdPort findUserByIdPort;

  @Autowired
  protected PasswordEncoder passwordEncoder;

  @Autowired
  protected JwtProvider jwtProvider;

  @Autowired
  protected DatabaseClient databaseClient;

  protected Mono<Void> cleanupUserFixtures() {
    return databaseClient.sql("DELETE FROM user_auth_providers")
        .fetch()
        .rowsUpdated()
        .then(databaseClient.sql("DELETE FROM users")
            .fetch()
            .rowsUpdated())
        .then();
  }

  protected User createUser(String email, String name) {
    return createUser(email, name, "password");
  }

  protected User createUser(String email, String name, String rawPassword) {
    User user = User.signUp(
        nextId(),
        email,
        name,
        passwordEncoder.encode(rawPassword));
    return createUserPort.createUser(user).block();
  }

  protected User createOAuthUser(
      String email,
      String name,
      AuthProvider provider,
      String providerUserId) {
    User user = createUserPort.createUser(User.signUpOAuth(nextId(), email, name))
        .block();
    createUserAuthProviderPort.createUserAuthProvider(UserAuthProvider.create(
        nextId(),
        user.id(),
        provider,
        providerUserId))
        .block();
    return user;
  }

  protected User getUser(String userId) {
    return findUserByIdPort.findUserById(userId).block();
  }

  protected User getUserByEmail(String email) {
    return findUserByEmailPort.findUserByEmail(email).block();
  }

  protected String generateAccessToken(String userId) {
    return jwtProvider.generateAccessToken(userId, new HashMap<>(),
        System.currentTimeMillis());
  }

  protected String generateRefreshToken(String userId) {
    return jwtProvider.generateRefreshToken(userId);
  }

  protected String nextId() {
    return UlidGenerator.generate();
  }

}
