package com.schemafy.core.common;

import com.schemafy.core.member.application.dto.SignUpCommand;
import com.schemafy.core.member.domain.entity.Member;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class TestFixture {
    public static PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static Mono<Member> createTestMember(String email, String name, String password) {
        String defaultEmail = Objects.requireNonNullElse(email, "test@example.com");
        String defaultName = Objects.requireNonNullElse(name, "Test User");
        String defaultPassword = Objects.requireNonNullElse(password, "encodedPassword");

        SignUpCommand command = new SignUpCommand(defaultEmail, defaultName, defaultPassword);
        return Member.signUp(command.toMemberInfo(), passwordEncoder);
    }
}
