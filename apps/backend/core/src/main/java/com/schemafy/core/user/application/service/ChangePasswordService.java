package com.schemafy.core.user.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.ChangePasswordCommand;
import com.schemafy.core.user.application.port.in.ChangePasswordUseCase;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.application.port.out.PasswordHashPort;
import com.schemafy.core.user.application.port.out.UpdateUserPasswordPort;
import com.schemafy.core.user.application.security.PasswordResetToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class ChangePasswordService implements ChangePasswordUseCase {

  private final FindUserByIdPort findUserByIdPort;
  private final PasswordHashPort passwordHashPort;
  private final UpdateUserPasswordPort updateUserPasswordPort;
  private final AuthTokenPort authTokenPort;

  @Override
  public Mono<Void> changePassword(String authenticatedUserId, ChangePasswordCommand command) {
    return authenticatedUserId == null
        ? changeWithResetToken(command)
        : changeAuthenticated(authenticatedUserId, command);
  }

  private Mono<Void> changeAuthenticated(String userId, ChangePasswordCommand command) {
    if (command.resetToken() != null || command.currentPassword() == null) {
      return Mono.error(new DomainException(UserErrorCode.INVALID_PARAMETER));
    }
    return findUserByIdPort.findUserById(userId)
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> {
          if (user.password() == null)
            return Mono.error(new DomainException(UserErrorCode.PASSWORD_CHANGE_UNAVAILABLE));
          return passwordHashPort.matches(command.currentPassword(), user.password())
              .filter(Boolean::booleanValue)
              .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.CURRENT_PASSWORD_INVALID)))
              .then(Mono.defer(() -> passwordHashPort.hash(command.newPassword())))
              .flatMap(hash -> updateUserPasswordPort.updateUserPassword(userId, hash));
        });
  }

  private Mono<Void> changeWithResetToken(ChangePasswordCommand command) {
    if (command.currentPassword() != null || command.resetToken() == null) {
      return Mono.error(new DomainException(UserErrorCode.INVALID_PARAMETER));
    }
    PasswordResetToken token = PasswordResetToken.parse(command.resetToken());
    return authTokenPort.consume(AuthTokenType.PASSWORD_RESET, token.userId(), token.rawToken())
        .filter(result -> result == AuthTokenConsumeResult.CONSUMED)
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.PASSWORD_RESET_TOKEN_INVALID)))
        .then(Mono.defer(() -> passwordHashPort.hash(command.newPassword())))
        .flatMap(hash -> updateUserPasswordPort.updateUserPassword(token.userId(), hash));
  }

}
