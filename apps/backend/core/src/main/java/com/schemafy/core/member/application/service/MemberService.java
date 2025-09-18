package com.schemafy.core.member.application.service;

import com.schemafy.core.member.application.dto.SignUpCommand;
import com.schemafy.core.member.presentation.dto.response.MemberInfoResponse;
import reactor.core.publisher.Mono;

public interface MemberService {
    Mono<MemberInfoResponse> signUp(SignUpCommand request);
    Mono<MemberInfoResponse> getMemberById(String memberId);
}
