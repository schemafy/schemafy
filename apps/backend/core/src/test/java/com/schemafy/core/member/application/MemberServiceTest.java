package com.schemafy.core.member.application;

import com.github.f4b6a3.ulid.UlidCreator;
import com.schemafy.core.common.TestFixture;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.member.application.dto.LoginCommand;
import com.schemafy.core.member.application.service.MemberService;
import com.schemafy.core.member.domain.entity.Member;
import com.schemafy.core.member.domain.repository.MemberRepository;
import com.schemafy.core.member.presentation.dto.request.SignUpRequest;
import com.schemafy.core.member.presentation.dto.response.MemberInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("MemberService 테스트")
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll().block();
    }

    @Test
    @DisplayName("회원가입에 성공한다")
    void signupSuccess() {
        // given
        SignUpRequest request = new SignUpRequest("test@example.com", "Test User", "password");

        // when
        Mono<MemberInfoResponse> result = memberService.signUp(request.toCommand());

        // then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.email().equals("test@example.com"))
                .verifyComplete();
    }

    @Test
    @DisplayName("회원가입시 이미 존재하는 이메일이면 실패한다")
    void signUp_fail_email_already_exists() {
        // given
        TestFixture.createTestMember("test@example.com", "Test User", "password")
                .flatMap(memberRepository::save)
                .block();

        SignUpRequest request = new SignUpRequest("test@example.com", "Test User", "password");

        // when
        Mono<MemberInfoResponse> result = memberService.signUp(request.toCommand());

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_ALREADY_EXISTS
                )
                .verify();
    }

    @Test
    @DisplayName("ID로 회원 조회에 성공한다")
    void getMemberByIdSuccess() {
        // given
        Member member = TestFixture.createTestMember("test@example.com", "Test User", "password")
                .flatMap(memberRepository::save)
                .block();

        // when
        Mono<MemberInfoResponse> result = memberService.getMemberById(member.getId());

        // then
        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.id()).isEqualTo(member.getId());
                    assertThat(res.email()).isEqualTo(member.getEmail()) ;
                    assertThat(res.name()).isEqualTo(member.getName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 회원은 조회에 실패한다")
    void getMemberById_fail_not_found() {
        // given
        String id = UlidCreator.getUlid().toString();

        // when
        Mono<MemberInfoResponse> result = memberService.getMemberById(id);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
                )
                .verify();
    }

    @Test
    @DisplayName("로그인에 성공한다")
    void login_success() {
        // given
        String rawPassword = "password";
        TestFixture.createTestMember("test@example.com", "Test User", rawPassword)
                .flatMap(memberRepository::save)
                .block();

        LoginCommand command = new LoginCommand("test@example.com", rawPassword);

        // when
        Mono<MemberInfoResponse> result = memberService.login(command);

        // then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.email().equals("test@example.com"))
                .verifyComplete();
    }

    @Test
    @DisplayName("로그인 시 존재하지 않는 이메일이면 실패한다")
    void login_fail_email_not_found() {
        // given
        LoginCommand command = new LoginCommand("nonexistent@example.com", "password");

        // when
        Mono<MemberInfoResponse> result = memberService.login(command);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
                )
                .verify();
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 틀리면 실패한다")
    void login_fail_password_mismatch() {
        // given
        TestFixture.createTestMember("test@example.com", "Test User", "password")
                .flatMap(memberRepository::save)
                .block();

        LoginCommand command = new LoginCommand("test@example.com", "wrong_password");

        // when
        Mono<MemberInfoResponse> result = memberService.login(command);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getErrorCode() == ErrorCode.LOGIN_FAILED
                )
                .verify();
    }
}
