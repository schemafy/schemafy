package com.schemafy.core.user.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.service.dto.LoginCommand;
import com.schemafy.core.user.service.dto.SignUpCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<User> signUp(SignUpCommand request) {
        return checkEmailUniqueness(request.email())
                .then(createNewUser(request));
    }

    private Mono<Void> checkEmailUniqueness(String email) {
        return userRepository.existsByEmail(email)
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.USER_ALREADY_EXISTS))
                        : Mono.empty());
    }

    private Mono<User> createNewUser(SignUpCommand request) {
        return User.signUp(request.toUserInfo(), passwordEncoder)
                .flatMap(userRepository::save)
                .onErrorMap(DuplicateKeyException.class,
                        e -> new BusinessException(ErrorCode.USER_ALREADY_EXISTS));
    }

    public Mono<UserInfoResponse> getUserById(String userId) {
        return userRepository.findById(userId)
                .map(UserInfoResponse::from)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)));
    }

    public Mono<User> login(LoginCommand command) {
        return findUserByEmail(command.email())
                .flatMap(user -> getUserByPasswordMatch(user, command.password()));
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
