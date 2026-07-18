package com.schemafy.core.user.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.SendSignUpEmailCodeCommand;
import com.schemafy.core.user.application.port.in.SendSignUpEmailCodeUseCase;
import com.schemafy.core.user.application.port.in.SignUpEmailVerificationResult;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.ExistsUserByEmailPort;
import com.schemafy.core.user.application.port.out.SendEmailVerificationPort;
import com.schemafy.core.user.application.security.VerificationCodeGenerator;
import com.schemafy.core.user.domain.AuthPolicy;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class SendSignUpEmailCodeService implements SendSignUpEmailCodeUseCase {

  private final ExistsUserByEmailPort existsUserByEmailPort;
  private final VerificationCodeGenerator verificationCodeGenerator;
  private final AuthTokenPort authTokenPort;
  private final SendEmailVerificationPort sendEmailVerificationPort;

  @Override
  public Mono<SignUpEmailVerificationResult> sendSignUpEmailCode(
      SendSignUpEmailCodeCommand command) {
    return existsUserByEmailPort.existsUserByEmail(command.email())
        .flatMap(exists -> exists
            ? Mono.error(new DomainException(UserErrorCode.ALREADY_EXISTS))
            : authTokenPort.findExpiresAt(
                AuthTokenType.EMAIL_VERIFICATION,
                command.email())
                .map(expiresAt -> new SignUpEmailVerificationResult(command.email(), expiresAt))
                .switchIfEmpty(Mono.defer(() -> issueVerificationCode(command.email()))));
  }

  private Mono<SignUpEmailVerificationResult> issueVerificationCode(String email) {
    String code = verificationCodeGenerator.generate();
    Instant expiresAt = Instant.now().plus(AuthPolicy.EMAIL_VERIFICATION_TTL);
    AuthToken token = new AuthToken(
        AuthTokenType.EMAIL_VERIFICATION,
        email,
        code,
        0,
        AuthPolicy.EMAIL_VERIFICATION_MAX_ATTEMPTS,
        expiresAt);

    return authTokenPort.saveIfAbsent(token)
        .flatMap(saved -> {
          if (saved) {
            return sendEmailVerificationPort.sendVerificationCode(email, code, expiresAt)
                .onErrorResume(error -> authTokenPort.delete(AuthTokenType.EMAIL_VERIFICATION, email)
                    .onErrorResume(deleteError -> Mono.empty())
                    .then(Mono.error(error)))
                .thenReturn(new SignUpEmailVerificationResult(email, expiresAt));
          }
          return authTokenPort.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, email)
              .map(existingExpiresAt -> new SignUpEmailVerificationResult(email, existingExpiresAt))
              .switchIfEmpty(Mono.defer(() -> issueVerificationCode(email)));
        });
  }

}
