package com.schemafy.core.member.domain.entity;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.member.domain.vo.MemberStatus;
import com.schemafy.core.member.domain.vo.Email;
import com.schemafy.core.member.domain.vo.MemberInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("members")
public class Member extends BaseEntity {

    private String email;

    private String name;

    private String password;

    private MemberStatus status;

    public static Mono<Member> signUp(MemberInfo memberInfo, PasswordEncoder passwordEncoder) {
        return Mono.fromCallable(() -> {
            // VO 검증
            Email email = new Email(memberInfo.email());

            String encodedPassword = passwordEncoder.encode(memberInfo.password());

            Member newMember = new Member(
                    email.address(),
                    memberInfo.name(),
                    encodedPassword,
                    MemberStatus.ACTIVE
            );
            newMember.generateId();

            return newMember;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
