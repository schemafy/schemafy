package com.schemafy.core.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.user.controller.dto.request.SignUpRequest;
import com.schemafy.core.user.repository.UserRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("UserService 트랜잭션 롤백 테스트")
class UserServiceRollbackTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    WorkspaceRepository workspaceRepository;

    @MockitoSpyBean
    WorkspaceMemberRepository workspaceMemberRepository;

    @BeforeEach
    void setUp() {
        Mockito.reset(workspaceMemberRepository);
        workspaceMemberRepository.deleteAll().block();
        workspaceRepository.deleteAll().block();
        userRepository.deleteAll().block();
    }

    @Test
    @DisplayName("WorkspaceMember 저장 실패 시 User와 Workspace 모두 롤백된다")
    void signUp_RollbackAll_WhenMemberSaveFails() {
        long initialUserCount = userRepository.count().block();
        long initialWorkspaceCount = workspaceRepository.count().block();
        long initialMemberCount = workspaceMemberRepository.count().block();

        // WorkspaceMember 저장 시 강제로 실패하도록 설정
        doReturn(Mono.error(new RuntimeException("DB 오류")))
                .when(workspaceMemberRepository)
                .save(any(WorkspaceMember.class));

        SignUpRequest request = new SignUpRequest(
                "rollback@example.com", "Rollback User", "password");

        StepVerifier.create(userService.signUp(request.toCommand()))
                .expectError(RuntimeException.class)
                .verify();

        assertThat(userRepository.count().block()).isEqualTo(initialUserCount);
        assertThat(workspaceRepository.count().block())
                .isEqualTo(initialWorkspaceCount);
        assertThat(workspaceMemberRepository.count().block())
                .isEqualTo(initialMemberCount);
    }

}
