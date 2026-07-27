package com.schemafy.core.user.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.LoginUserCommand;
import com.schemafy.core.user.application.port.in.LoginUserUseCase;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;
import com.schemafy.core.user.application.port.out.PasswordHashPort;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class LoginUserService implements LoginUserUseCase {

  private final FindUserByEmailPort findUserByEmailPort;
  private final PasswordHashPort passwordHashPort;

  @Override
  public Mono<User> loginUser(LoginUserCommand command) {
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .flatMap(email -> findUserByEmailPort.findUserByEmail(email.address())
            .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
            .flatMap(user -> {
              if (user.password() == null) {
                return Mono.error(new DomainException(UserErrorCode.LOGIN_FAILED));
              }
              return passwordHashPort.matches(command.password(), user.password())
                  .filter(Boolean::booleanValue)
                  .map(matches -> user)
                  .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.LOGIN_FAILED)));
            }));
  }

}
