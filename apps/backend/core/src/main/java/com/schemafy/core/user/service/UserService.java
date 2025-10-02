package com.schemafy.core.user.service;

import com.github.f4b6a3.ulid.Ulid;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.util.ULIDUtils;
import com.schemafy.core.user.service.dto.LoginCommand;
import com.schemafy.core.user.service.dto.SignUpCommand;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<UserInfoResponse> signUp(SignUpCommand request) {
        return checkEmailUniqueness(request.email())
                .then(createNewUser(request))
                .map(UserInfoResponse::from);
    }

    private Mono<Void> checkEmailUniqueness(String email) {
        return userRepository.existsByEmail(email)
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.USER_ALREADY_EXISTS))
                        : Mono.empty());
    }

    private Mono<User> createNewUser(SignUpCommand request) {
        Ulid id = ULIDUtils.generateUlid();

        return User.signUp(id, request.toUserInfo(), passwordEncoder)
                .flatMap(userRepository::save)
                .onErrorMap(DuplicateKeyException.class,
                        e -> new BusinessException(ErrorCode.USER_ALREADY_EXISTS));
    }

    public Mono<UserInfoResponse> getUserById(String userId) {
        return userRepository.findById(userId)
                .doOnNext(user -> {
                    log.info("user: {}", user);
                })
                .map(UserInfoResponse::from)
                .doOnError(e -> log.error("getUserById 오류: userId={}, error={}", userId, e.getMessage(), e))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)));
    }

    public Mono<UserInfoResponse> login(LoginCommand command) {
        return findUserByEmail(command.email())
                .flatMap(user -> getUserByPasswordMatch(user, command.password()))
                .map(UserInfoResponse::from);
    }

    private Mono<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)));
    }

    private Mono<User> getUserByPasswordMatch(User user, String password) {
        return user.matchesPassword(password, passwordEncoder)
                .filter(Boolean::booleanValue)
                .map(matches -> user)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.LOGIN_FAILED)));
    }
}
