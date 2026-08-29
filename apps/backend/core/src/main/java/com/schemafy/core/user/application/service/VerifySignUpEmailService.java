package com.schemafy.core.user.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.VerifySignUpEmailCommand;
import com.schemafy.core.user.application.port.in.VerifySignUpEmailResult;
import com.schemafy.core.user.application.port.in.VerifySignUpEmailUseCase;
import com.schemafy.core.user.application.port.out.AuthMailPolicyPort;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.ExistsUserByEmailPort;
import com.schemafy.core.user.application.security.SignupVerificationTokenGenerator;
import com.schemafy.core.user.domain.AuthPolicy;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class VerifySignUpEmailService implements VerifySignUpEmailUseCase {

  private final ExistsUserByEmailPort existsUserByEmailPort;
  private final AuthTokenPort authTokenPort;
  private final SignupVerificationTokenGenerator signupVerificationTokenGenerator;
  private final AuthMailPolicyPort authMailPolicyPort;

  @Override
  public Mono<VerifySignUpEmailResult> verifySignUpEmail(
      VerifySignUpEmailCommand command) {
    if (!authMailPolicyPort.isEnabled()) {
      return Mono.error(new DomainException(UserErrorCode.AUTH_MAIL_DISABLED));
    }
    return existsUserByEmailPort.existsUserByEmail(command.email())
        .flatMap(exists -> exists
            ? Mono.error(new DomainException(UserErrorCode.ALREADY_EXISTS))
            : consumeEmailVerification(command)
                .then(Mono.defer(() -> issueSignupVerification(command.email()))));
  }

  private Mono<Void> consumeEmailVerification(VerifySignUpEmailCommand command) {
    return authTokenPort.consume(AuthTokenType.EMAIL_VERIFICATION,
        command.email(), command.code())
        .flatMap(this::handleConsumeResult);
  }

  private Mono<Void> handleConsumeResult(AuthTokenConsumeResult result) {
    return switch (result) {
    case CONSUMED -> Mono.empty();
    case MISSING -> Mono.error(new DomainException(
        UserErrorCode.VERIFICATION_CODE_EXPIRED));
    case MISMATCH -> Mono.error(new DomainException(
        UserErrorCode.VERIFICATION_CODE_INVALID));
    case ATTEMPTS_EXCEEDED -> Mono.error(new DomainException(
        UserErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED));
    };
  }

  private Mono<VerifySignUpEmailResult> issueSignupVerification(String email) {
    String token = signupVerificationTokenGenerator.generate();
    Instant expiresAt = Instant.now().plus(AuthPolicy.SIGNUP_VERIFICATION_TTL);
    AuthToken authToken = new AuthToken(
        AuthTokenType.SIGNUP_VERIFICATION,
        email,
        token,
        0,
        AuthPolicy.SIGNUP_VERIFICATION_MAX_ATTEMPTS,
        expiresAt);

    return authTokenPort.save(authToken)
        .thenReturn(new VerifySignUpEmailResult(email, token, expiresAt));
  }

}
