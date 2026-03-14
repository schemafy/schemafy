package com.schemafy.core.user.application.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.application.port.in.LoginOrSignUpOAuthCommand;
import com.schemafy.core.user.application.port.in.LoginOrSignUpOAuthResult;
import com.schemafy.core.user.application.port.in.LoginOrSignUpOAuthUseCase;
import com.schemafy.core.user.application.port.out.CreateUserAuthProviderPort;
import com.schemafy.core.user.application.port.out.CreateUserPort;
import com.schemafy.core.user.application.port.out.FindUserAuthProviderPort;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserAuthProvider;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class LoginOrSignUpOAuthService implements LoginOrSignUpOAuthUseCase {

  private final TransactionalOperator transactionalOperator;
  private final FindUserAuthProviderPort findUserAuthProviderPort;
  private final FindUserByEmailPort findUserByEmailPort;
  private final FindUserByIdPort findUserByIdPort;
  private final CreateUserPort createUserPort;
  private final CreateUserAuthProviderPort createUserAuthProviderPort;
  private final UlidGeneratorPort ulidGeneratorPort;

  @Override
  public Mono<LoginOrSignUpOAuthResult> loginOrSignUpOAuth(
      LoginOrSignUpOAuthCommand command) {
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .flatMap(email -> {
          LoginOrSignUpOAuthCommand normalizedCommand = command.withEmail(
              email.address());
          return findUserAuthProviderPort.findUserAuthProvider(
              normalizedCommand.provider(), normalizedCommand.providerUserId())
              .flatMap(authProvider -> findUserByIdPort.findUserById(authProvider.userId()))
              .map(user -> new LoginOrSignUpOAuthResult(user, false))
              .switchIfEmpty(linkOrCreateOAuthUser(normalizedCommand, email));
        })
        .as(transactionalOperator::transactional);
  }

  private Mono<LoginOrSignUpOAuthResult> linkOrCreateOAuthUser(
      LoginOrSignUpOAuthCommand command,
      Email email) {
    return findUserByEmailPort.findUserByEmail(email.address())
        .flatMap(existingUser -> linkExistingUserToOAuthIdempotent(existingUser, command)
            .map(linked -> new LoginOrSignUpOAuthResult(linked, false)))
        .switchIfEmpty(Mono.defer(() -> createOAuthUser(command)));
  }

  private Mono<User> linkExistingUserToOAuthIdempotent(
      User existingUser,
      LoginOrSignUpOAuthCommand command) {
    return createUserAuthProviderPort.createUserAuthProvider(
        newUserAuthProvider(existingUser.id(), command))
        .thenReturn(existingUser)
        .onErrorResume(DuplicateKeyException.class,
            e -> resolveLinkedUserForDuplicateProvider(command));
  }

  private Mono<User> resolveLinkedUserForDuplicateProvider(
      LoginOrSignUpOAuthCommand command) {
    return findUserAuthProviderPort.findUserAuthProvider(
        command.provider(), command.providerUserId())
        .switchIfEmpty(Mono.error(new DomainException(
            UserErrorCode.OAUTH_LINK_INCONSISTENT)))
        .flatMap(authProvider -> findUserByIdPort.findUserById(authProvider.userId()))
        .switchIfEmpty(Mono.error(new DomainException(
            UserErrorCode.OAUTH_LINK_INCONSISTENT)));
  }

  private Mono<LoginOrSignUpOAuthResult> createOAuthUser(
      LoginOrSignUpOAuthCommand command) {
    return Mono.fromCallable(ulidGeneratorPort::generate)
        .map(userId -> User.signUpOAuth(
            userId,
            command.email(),
            command.name()))
        .flatMap(createUserPort::createUser)
        .onErrorMap(DuplicateKeyException.class,
            e -> new DomainException(UserErrorCode.ALREADY_EXISTS))
        .flatMap(savedUser -> createUserAuthProviderPort
            .createUserAuthProvider(newUserAuthProvider(savedUser.id(), command))
            .thenReturn(new LoginOrSignUpOAuthResult(savedUser, true)));
  }

  private UserAuthProvider newUserAuthProvider(
      String userId,
      LoginOrSignUpOAuthCommand command) {
    return UserAuthProvider.create(
        ulidGeneratorPort.generate(),
        userId,
        command.provider(),
        command.providerUserId());
  }

}
