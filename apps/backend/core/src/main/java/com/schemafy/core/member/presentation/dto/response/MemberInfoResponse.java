package com.schemafy.core.member.presentation.dto.response;

import com.schemafy.core.member.domain.entity.Member;

public record MemberInfoResponse(String id, String email, String name) {
    public static MemberInfoResponse from(Member member) {
        return new MemberInfoResponse(member.getId(), member.getEmail(), member.getName());
    }
}
