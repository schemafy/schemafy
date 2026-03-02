package com.schemafy.core.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.AuthErrorCode;
import com.schemafy.core.common.exception.CommonErrorCode;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.service.user.UserApiCommandMapper;
import com.schemafy.core.user.service.user.UserApiResponseMapper;
import com.schemafy.core.user.service.user.UserWorkspaceProvisioner;
import com.schemafy.core.user.service.dto.LoginCommand;
import com.schemafy.core.user.service.dto.OAuthLoginCommand;
import com.schemafy.core.user.service.dto.SignUpCommand;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.user.application.port.in.GetUserByIdUseCase;
import com.schemafy.domain.user.application.port.in.LoginOrSignUpOAuthUseCase;
import com.schemafy.domain.user.application.port.in.LoginUserUseCase;
import com.schemafy.domain.user.application.port.in.SignUpUserUseCase;
import com.schemafy.domain.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

  private final TransactionalOperator transactionalOperator;
  private final SignUpUserUseCase signUpUserUseCase;
  private final LoginUserUseCase loginUserUseCase;
  private final GetUserByIdUseCase getUserByIdUseCase;
  private final LoginOrSignUpOAuthUseCase loginOrSignUpOAuthUseCase;
  private final UserApiCommandMapper commandMapper;
  private final UserApiResponseMapper responseMapper;
  private final UserWorkspaceProvisioner workspaceProvisioner;
  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;

  public Mono<User> signUp(SignUpCommand request) {
    return signUpUserUseCase.signUpUser(
            commandMapper.toSignUpUserCommand(request))
        .flatMap(workspaceProvisioner::createDefaultWorkspace)
        .flatMap(this::findCoreUserById)
        .as(transactionalOperator::transactional);
  }

  public Mono<User> loginOrSignUpOAuth(OAuthLoginCommand command) {
    return loginOrSignUpOAuthUseCase.loginOrSignUpOAuth(
            commandMapper.toLoginOrSignUpOAuthCommand(command))
        .flatMap(result -> result.newUser()
            ? workspaceProvisioner.createDefaultWorkspace(result.user())
            : Mono.just(result.user()))
        .flatMap(this::findCoreUserById)
        .onErrorMap(this::remapOAuthInconsistentError)
        .as(transactionalOperator::transactional);
  }

  public Mono<UserInfoResponse> getUserById(String userId) {
    return getUserByIdUseCase.getUserById(commandMapper.toGetUserByIdQuery(userId))
        .map(responseMapper::toUserInfoResponse);
  }

  public Mono<User> login(LoginCommand command) {
    return loginUserUseCase.loginUser(commandMapper.toLoginUserCommand(command))
        .flatMap(this::findCoreUserById);
  }

  public Mono<User> getUserFromRefreshToken(String refreshToken) {
    return Mono.fromCallable(() -> {
          String userId = jwtProvider.extractUserId(refreshToken);
          String tokenType = jwtProvider.getTokenType(refreshToken);

          if (!JwtProvider.REFRESH_TOKEN.equals(tokenType)) {
            throw new DomainException(AuthErrorCode.INVALID_TOKEN_TYPE);
          }

          if (!jwtProvider.validateToken(refreshToken, userId)) {
            throw new DomainException(AuthErrorCode.INVALID_REFRESH_TOKEN);
          }

          return userId;
        })
        .flatMap(userRepository::findById)
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)))
        .onErrorMap(e -> !(e instanceof DomainException),
            e -> new DomainException(
                AuthErrorCode.INVALID_REFRESH_TOKEN));
  }

  private Mono<User> findCoreUserById(com.schemafy.domain.user.domain.User user) {
    return userRepository.findById(user.id())
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)));
  }

  private Throwable remapOAuthInconsistentError(Throwable throwable) {
    if (!(throwable instanceof DomainException domainException)) {
      return throwable;
    }
    if (domainException.getErrorCode() == UserErrorCode.OAUTH_LINK_INCONSISTENT) {
      return new DomainException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
    return throwable;
  }

}
