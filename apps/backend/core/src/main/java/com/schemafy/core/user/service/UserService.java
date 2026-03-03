package com.schemafy.core.user.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.AuthErrorCode;
import com.schemafy.core.common.exception.CommonErrorCode;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import com.schemafy.core.user.exception.UserErrorCode;
import com.schemafy.core.user.repository.UserAuthProviderRepository;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.entity.UserAuthProvider;
import com.schemafy.core.user.service.dto.LoginCommand;
import com.schemafy.core.user.service.dto.OAuthLoginCommand;
import com.schemafy.core.user.service.dto.SignUpCommand;
import com.schemafy.domain.common.exception.DomainException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final TransactionalOperator transactionalOperator;
  private final UserRepository userRepository;
  private final UserAuthProviderRepository userAuthProviderRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;

  public Mono<User> signUp(SignUpCommand request) {
    return checkEmailUniqueness(request.email())
        .then(createNewUser(request))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> checkEmailUniqueness(String email) {
    return userRepository.existsByEmail(email)
        .flatMap(exists -> exists
            ? Mono.error(new DomainException(
                UserErrorCode.ALREADY_EXISTS))
            : Mono.empty());
  }

  private Mono<User> createNewUser(SignUpCommand request) {
    return User.signUp(request.toUserInfo(), passwordEncoder)
        .flatMap(userRepository::save)
        .onErrorMap(DuplicateKeyException.class,
            e -> new DomainException(
                UserErrorCode.ALREADY_EXISTS));
  }

  public Mono<User> loginOrSignUpOAuth(OAuthLoginCommand command) {
    return userAuthProviderRepository
        .findByProviderAndProviderUserId(
            command.provider().name(), command.providerUserId())
        .flatMap(
            authProvider -> userRepository.findById(authProvider.getUserId()))
        .switchIfEmpty(linkOrCreateOAuthUser(command))
        .as(transactionalOperator::transactional);
  }

  private Mono<User> linkOrCreateOAuthUser(OAuthLoginCommand command) {
    return userRepository.findByEmail(command.email())
        .flatMap(existingUser -> linkExistingUserToOAuthIdempotent(
            existingUser, command))
        .switchIfEmpty(Mono.defer(() -> createOAuthUser(command)));
  }

  private Mono<User> linkExistingUserToOAuthIdempotent(User existingUser,
      OAuthLoginCommand command) {
    return saveAuthProvider(existingUser, command)
        .thenReturn(existingUser)
        .onErrorResume(DuplicateKeyException.class, e -> {
          log.warn(
              "OAuth provider link already exists during auto-link. provider={}, providerUserId={}",
              command.provider(), command.providerUserId());
          return userAuthProviderRepository.findByProviderAndProviderUserId(
              command.provider().name(), command.providerUserId())
              .switchIfEmpty(Mono.defer(() -> {
                log.error(
                    "OAuth provider link duplicate detected but provider row not found on re-read. provider={}, providerUserId={}",
                    command.provider(), command.providerUserId(), e);
                return Mono.error(
                    new DomainException(
                        CommonErrorCode.INTERNAL_SERVER_ERROR));
              }))
              .flatMap(provider -> userRepository.findById(provider.getUserId()))
              .switchIfEmpty(Mono.defer(() -> {
                log.error(
                    "OAuth provider link duplicate detected but linked user not found. provider={}, providerUserId={}",
                    command.provider(), command.providerUserId(), e);
                return Mono.error(
                    new DomainException(
                        CommonErrorCode.INTERNAL_SERVER_ERROR));
              }));
        });
  }

  private Mono<User> createOAuthUser(OAuthLoginCommand command) {
    User newUser = User.signUpOAuth(command.email(), command.name());
    return userRepository.save(newUser)
        .onErrorMap(DuplicateKeyException.class,
            e -> new DomainException(
                UserErrorCode.ALREADY_EXISTS))
        .flatMap(savedUser -> saveAuthProvider(savedUser, command)
            .thenReturn(savedUser));
  }

  private Mono<UserAuthProvider> saveAuthProvider(User user,
      OAuthLoginCommand command) {
    UserAuthProvider authProvider = UserAuthProvider.create(
        user.getId(), command.provider(), command.providerUserId());
    return userAuthProviderRepository.save(authProvider);
  }

  public Mono<UserInfoResponse> getUserById(String userId) {
    return userRepository.findById(userId)
        .map(UserInfoResponse::from)
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)));
  }

  public Mono<User> login(LoginCommand command) {
    return findUserByEmail(command.email())
        .flatMap(user -> getUserByPasswordMatch(user,
            command.password()));
  }

  private Mono<User> findUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)));
  }

  private Mono<User> getUserByPasswordMatch(User user, String password) {
    return user.matchesPassword(password, passwordEncoder)
        .filter(Boolean::booleanValue)
        .map(matches -> user)
        .switchIfEmpty(Mono
            .error(new DomainException(UserErrorCode.LOGIN_FAILED)));
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

}
