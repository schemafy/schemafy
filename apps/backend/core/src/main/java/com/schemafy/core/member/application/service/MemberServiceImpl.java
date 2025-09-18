package com.schemafy.core.member.application.service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.member.application.dto.LoginCommand;
import com.schemafy.core.member.application.dto.SignUpCommand;
import com.schemafy.core.member.domain.entity.Member;
import com.schemafy.core.member.domain.repository.MemberRepository;
import com.schemafy.core.member.presentation.dto.response.MemberInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<MemberInfoResponse> signUp(SignUpCommand request) {
        return checkEmailUniqueness(request.email())
                .then(createNewMember(request))
                .map(MemberInfoResponse::from);
    }

    private Mono<Void> checkEmailUniqueness(String email) {
        return memberRepository.existsByEmail(email)
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS))
                        : Mono.empty());
    }

    private Mono<Member> createNewMember(SignUpCommand request) {
        return Member.signUp(request.toMemberInfo(), passwordEncoder)
                .flatMap(memberRepository::save)
                .onErrorMap(DuplicateKeyException.class,
                        e -> new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS));
    }

    public Mono<MemberInfoResponse> getMemberById(String memberId) {
        return memberRepository.findById(memberId)
                .map(MemberInfoResponse::from)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));
    }

    @Override
    public Mono<MemberInfoResponse> login(LoginCommand command) {
        return findMemberByEmail(command.email())
                .flatMap(member -> validatePassword(member, command.password()))
                .map(MemberInfoResponse::from);
    }

    private Mono<Member> findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));
    }

    private Mono<Member> validatePassword(Member member, String password) {
        return member.matchesPassword(password, passwordEncoder)
                .filter(Boolean::booleanValue)
                .map(matches -> member)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.LOGIN_FAILED)));
    }
}
