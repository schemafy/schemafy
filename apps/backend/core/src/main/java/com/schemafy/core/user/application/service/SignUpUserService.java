package com.schemafy.core.user.application.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.application.port.in.SignUpUserCommand;
import com.schemafy.core.user.application.port.in.SignUpUserUseCase;
import com.schemafy.core.user.application.port.out.CreateUserPort;
import com.schemafy.core.user.application.port.out.ExistsUserByEmailPort;
import com.schemafy.core.user.application.port.out.PasswordHashPort;
import com.schemafy.core.user.domain.Email;
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

  @Override
  public Mono<User> signUpUser(SignUpUserCommand command) {
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .map(email -> command.withEmail(email.address()))
        .flatMap(normalizedCommand -> existsUserByEmailPort
            .existsUserByEmail(normalizedCommand.email())
            .flatMap(exists -> exists
                ? Mono.error(new DomainException(UserErrorCode.ALREADY_EXISTS))
                : createNewUser(normalizedCommand)))
        .onErrorMap(DuplicateKeyException.class,
            e -> new DomainException(UserErrorCode.ALREADY_EXISTS))
        .as(transactionalOperator::transactional);
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
