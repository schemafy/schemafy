package com.schemafy.core.member.application.dto;

import com.schemafy.core.member.domain.vo.MemberInfo;

public record SignUpCommand(String email, String name, String password) {
    public MemberInfo toMemberInfo() {
        return new MemberInfo(email, name, password);
    }
}
