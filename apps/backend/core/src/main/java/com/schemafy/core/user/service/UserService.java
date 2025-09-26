package com.schemafy.core.user.service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.user.service.dto.LoginCommand;
import com.schemafy.core.user.service.dto.SignUpCommand;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
                        ? Mono.error(new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS))
                        : Mono.empty());
    }

    private Mono<User> createNewUser(SignUpCommand request) {
        return User.signUp(request.toUserInfo(), passwordEncoder)
                .flatMap(userRepository::save)
                .onErrorMap(DuplicateKeyException.class,
                        e -> new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS));
    }

    public Mono<UserInfoResponse> getUserById(String userId) {
        return userRepository.findById(userId)
                .map(UserInfoResponse::from)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));
    }

    public Mono<UserInfoResponse> login(LoginCommand command) {
        return findUserByEmail(command.email())
                .flatMap(member -> validatePassword(member, command.password()))
                .map(UserInfoResponse::from);
    }

    private Mono<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));
    }

    private Mono<User> validatePassword(User user, String password) {
        return user.matchesPassword(password, passwordEncoder)
                .filter(Boolean::booleanValue)
                .map(matches -> user)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.LOGIN_FAILED)));
    }
}
