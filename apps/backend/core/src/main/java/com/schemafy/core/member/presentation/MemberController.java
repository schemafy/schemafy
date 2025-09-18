package com.schemafy.core.member.presentation;

import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.member.application.service.MemberService;
import com.schemafy.core.member.presentation.dto.request.LoginRequest;
import com.schemafy.core.member.presentation.dto.request.SignUpRequest;
import com.schemafy.core.member.presentation.dto.response.MemberInfoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/signup")
    public Mono<BaseResponse<MemberInfoResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        return memberService.signUp(request.toCommand())
                .map(BaseResponse::success);
    }

    @PostMapping("/login")
    public Mono<BaseResponse<MemberInfoResponse>> login(@Valid @RequestBody LoginRequest request) {
        return memberService.login(request.toCommand())
                .map(BaseResponse::success);
    }

    @GetMapping("/{memberId}")
    public Mono<BaseResponse<MemberInfoResponse>> getMember(@PathVariable String memberId) {
        return memberService.getMemberById(memberId)
                .map(BaseResponse::success);
    }
}
