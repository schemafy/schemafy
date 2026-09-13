package com.schemafy.core.user.application.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.application.port.in.SignUpUserCommand;
import com.schemafy.core.user.application.port.in.SignUpUserUseCase;
import com.schemafy.core.user.application.port.out.AuthMailPolicyPort;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.CreateUserPort;
import com.schemafy.core.user.application.port.out.ExistsUserByEmailPort;
import com.schemafy.core.user.application.port.out.PasswordHashPort;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class SignUpUserService implements SignUpUserUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ExistsUserByEmailPort existsUserByEmailPort;
  private final PasswordHashPort passwordHashPort;
  private final CreateUserPort createUserPort;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final AuthTokenPort authTokenPort;
  private final AuthMailPolicyPort authMailPolicyPort;

  @Override
  public Mono<User> signUpUser(SignUpUserCommand command) {
    return existsUserByEmailPort.existsUserByEmail(command.email())
        .flatMap(exists -> exists
            ? Mono.error(new DomainException(UserErrorCode.ALREADY_EXISTS))
            : consumeSignupVerification(command)
                .then(Mono.defer(() -> createNewUser(command))))
        .onErrorMap(DuplicateKeyException.class,
            e -> new DomainException(UserErrorCode.ALREADY_EXISTS))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> consumeSignupVerification(SignUpUserCommand command) {
    // 인증 메일이 비활성화된 환경에서는 코드를 전달할 수 없으므로 가입 검증 생략.
    if (!authMailPolicyPort.isEnabled()) {
      return Mono.empty();
    }
    if (command.signupVerificationToken() == null
        || command.signupVerificationToken().isBlank()) {
      return Mono.error(new DomainException(UserErrorCode.EMAIL_NOT_VERIFIED));
    }
    return authTokenPort.consume(
        AuthTokenType.SIGNUP_VERIFICATION,
        command.email(),
        command.signupVerificationToken())
        .flatMap(this::handleConsumeResult);
  }

  private Mono<Void> handleConsumeResult(AuthTokenConsumeResult result) {
    return switch (result) {
    case CONSUMED -> Mono.empty();
    case MISSING -> Mono.error(new DomainException(
        UserErrorCode.SIGNUP_VERIFICATION_INVALID));
    case MISMATCH -> Mono.error(new DomainException(
        UserErrorCode.VERIFICATION_CODE_INVALID));
    case ATTEMPTS_EXCEEDED -> Mono.error(new DomainException(
        UserErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED));
    };
  }

  private Mono<User> createNewUser(SignUpUserCommand command) {
    return Mono.fromCallable(ulidGeneratorPort::generate)
        .zipWith(passwordHashPort.hash(command.password()))
        .map(tuple -> User.signUp(
            tuple.getT1(),
            command.email(),
            command.name(),
            tuple.getT2()))
        .flatMap(createUserPort::createUser);
  }

}
