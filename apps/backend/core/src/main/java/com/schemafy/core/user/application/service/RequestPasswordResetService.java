package com.schemafy.core.user.application.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.RequestPasswordResetCommand;
import com.schemafy.core.user.application.port.in.RequestPasswordResetUseCase;
import com.schemafy.core.user.application.port.out.AuthMailPolicyPort;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;
import com.schemafy.core.user.application.port.out.SendPasswordResetEmailPort;
import com.schemafy.core.user.application.security.PasswordResetToken;
import com.schemafy.core.user.application.security.SignupVerificationTokenGenerator;
import com.schemafy.core.user.domain.AuthPolicy;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Slf4j
@RequiredArgsConstructor
class RequestPasswordResetService implements RequestPasswordResetUseCase {

  private final AuthMailPolicyPort authMailPolicyPort;
  private final FindUserByEmailPort findUserByEmailPort;
  private final AuthTokenPort authTokenPort;
  private final SignupVerificationTokenGenerator tokenGenerator;
  private final SendPasswordResetEmailPort sendPasswordResetEmailPort;

  @Override
  public Mono<Void> requestPasswordReset(RequestPasswordResetCommand command) {
    if (!authMailPolicyPort.isEnabled()) {
      return Mono.error(new DomainException(UserErrorCode.AUTH_MAIL_DISABLED));
    }
    return Mono.fromSupplier(() -> Email.from(command.email()).address())
        .flatMap(email -> findUserByEmailPort.findUserByEmail(email)
            .filter(user -> user.password() != null)
            .flatMap(user -> authTokenPort.findExpiresAt(AuthTokenType.PASSWORD_RESET, user.id())
                .hasElement()
                .flatMap(exists -> exists ? Mono.empty() : issue(user.id(), user.email())))
            .then());
  }

  private Mono<Void> issue(String userId, String email) {
    return Mono.defer(() -> {
      String rawToken = tokenGenerator.generate();
      Instant expiresAt = Instant.now().plus(AuthPolicy.PASSWORD_RESET_TTL);
      AuthToken token = new AuthToken(AuthTokenType.PASSWORD_RESET, userId, rawToken, 0,
          AuthPolicy.PASSWORD_RESET_MAX_ATTEMPTS, expiresAt);
      return authTokenPort.saveIfAbsent(token)
          .flatMap(saved -> saved
              ? sendPasswordResetEmailPort.sendPasswordResetLink(
                  email, PasswordResetToken.of(userId, rawToken).value(), expiresAt)
                  .doOnError(error -> log.warn(
                      "Password reset email delivery failed for userId={}", userId, error))
                  .onErrorResume(error -> authTokenPort.delete(AuthTokenType.PASSWORD_RESET, userId)
                      .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(50)))
                      .doOnError(deleteError -> log.warn(
                          "Failed to clean up password reset token after email delivery failure",
                          deleteError))
                      .onErrorResume(deleteError -> Mono.empty())
                      .then())
              : Mono.empty());
    });
  }

}
