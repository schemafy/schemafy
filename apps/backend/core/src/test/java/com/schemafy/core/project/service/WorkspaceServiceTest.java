package com.schemafy.core.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.project.controller.dto.request.AddWorkspaceMemberRequest;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.request.UpdateMemberRoleRequest;
import com.schemafy.core.project.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceResponse;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("WorkspaceService 테스트")
class WorkspaceServiceTest {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User memberUser;
    private User outsiderUser;
    private Workspace testWorkspace;
    private WorkspaceMember adminMember;
    private WorkspaceMember normalMember;

    @BeforeEach
    void setUp() {
        Mono.when(
                workspaceMemberRepository.deleteAll(),
                workspaceRepository.deleteAll(),
                userRepository.deleteAll()).block();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        adminUser = User.signUp(
                new UserInfo("admin@test.com", "Admin User", "password"),
                encoder).flatMap(userRepository::save).block();

        memberUser = User.signUp(
                new UserInfo("member@test.com", "Member User", "password"),
                encoder).flatMap(userRepository::save).block();

        outsiderUser = User.signUp(
                new UserInfo("outsider@test.com", "Outsider User", "password"),
                encoder).flatMap(userRepository::save).block();

        testWorkspace = Workspace.create(
                adminUser.getId(),
                "Test Workspace",
                "Test Description",
                WorkspaceSettings.defaultSettings());
        testWorkspace = workspaceRepository.save(testWorkspace).block();

        adminMember = WorkspaceMember.create(
                testWorkspace.getId(), adminUser.getId(), WorkspaceRole.ADMIN);
        adminMember = workspaceMemberRepository.save(adminMember).block();

        normalMember = WorkspaceMember.create(
                testWorkspace.getId(), memberUser.getId(),
                WorkspaceRole.MEMBER);
        normalMember = workspaceMemberRepository.save(normalMember).block();
    }

    @Nested
    @DisplayName("워크스페이스 생성")
    class CreateWorkspace {

        @Test
        @DisplayName("워크스페이스 생성 시 멤버도 함께 생성된다")
        void createWorkspace_CreatesOwnerMember() {
            CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                    "New Workspace",
                    "New Description",
                    null);

            Mono<WorkspaceResponse> result = workspaceService.createWorkspace(
                    request, outsiderUser.getId());

            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.name()).isEqualTo("New Workspace");
                        assertThat(response.description())
                                .isEqualTo("New Description");
                    })
                    .verifyComplete();

            StepVerifier.create(workspaceMemberRepository
                    .findByUserIdAndNotDeleted(outsiderUser.getId())
                    .collectList())
                    .assertNext(members -> {
                        assertThat(members).hasSize(1);
                        assertThat(members.get(0).isAdmin()).isTrue();
                    })
                    .verifyComplete();
        }

    }

    @Nested
    @DisplayName("워크스페이스 삭제")
    class DeleteWorkspace {

        @Test
        @DisplayName("워크스페이스 삭제 시 모든 멤버도 soft-delete 된다")
        void deleteWorkspace_SoftDeletesAllMembers() {
            // 추가 워크스페이스 생성 (기본 워크스페이스는 삭제 불가하므로)
            Workspace newWorkspace = Workspace.create(
                    adminUser.getId(),
                    "Deletable Workspace",
                    "Description",
                    WorkspaceSettings.defaultSettings());
            newWorkspace = workspaceRepository.save(newWorkspace).block();

            WorkspaceMember member1 = WorkspaceMember.create(
                    newWorkspace.getId(), adminUser.getId(),
                    WorkspaceRole.ADMIN);
            member1 = workspaceMemberRepository.save(member1).block();

            WorkspaceMember member2 = WorkspaceMember.create(
                    newWorkspace.getId(), memberUser.getId(),
                    WorkspaceRole.MEMBER);
            member2 = workspaceMemberRepository.save(member2).block();

            String workspaceId = newWorkspace.getId();

            Mono<Void> result = workspaceService.deleteWorkspace(
                    workspaceId, adminUser.getId());

            StepVerifier.create(result).verifyComplete();

            Workspace deleted = workspaceRepository.findById(workspaceId)
                    .block();
            assertThat(deleted.isDeleted()).isTrue();

            StepVerifier.create(workspaceMemberRepository
                    .findByWorkspaceIdAndNotDeleted(workspaceId, 100, 0)
                    .collectList())
                    .assertNext(members -> assertThat(members).isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("이미 삭제된 워크스페이스 삭제 시 에러 발생")
        void deleteWorkspace_AlreadyDeleted_Fails() {
            Workspace newWorkspace = Workspace.create(
                    adminUser.getId(),
                    "Will Delete",
                    "Description",
                    WorkspaceSettings.defaultSettings());
            newWorkspace = workspaceRepository.save(newWorkspace).block();

            WorkspaceMember member = WorkspaceMember.create(
                    newWorkspace.getId(), adminUser.getId(),
                    WorkspaceRole.ADMIN);
            workspaceMemberRepository.save(member).block();

            workspaceService.deleteWorkspace(newWorkspace.getId(),
                    adminUser.getId()).block();

            // 삭제 후 멤버도 soft-delete되므로 WORKSPACE_ACCESS_DENIED 발생
            StepVerifier.create(workspaceService.deleteWorkspace(
                    newWorkspace.getId(), adminUser.getId()))
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.WORKSPACE_ACCESS_DENIED)
                    .verify();
        }

        @Test
        @DisplayName("ADMIN이 아닌 멤버는 삭제 불가")
        void deleteWorkspace_NonAdmin_Rejected() {
            StepVerifier.create(workspaceService.deleteWorkspace(
                    testWorkspace.getId(), memberUser.getId()))
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.WORKSPACE_ADMIN_REQUIRED)
                    .verify();
        }

    }

    @Nested
    @DisplayName("멤버 관리")
    class MemberManagement {

        @Test
        @DisplayName("마지막 ADMIN 제거 시 에러 발생")
        void removeMember_LastAdmin_Prevented() {
            // 일반 멤버만 제거하고 ADMIN 1명만 남김
            workspaceMemberRepository.deleteById(normalMember.getId()).block();

            StepVerifier.create(workspaceService.removeMember(
                    testWorkspace.getId(), adminMember.getId(),
                    adminUser.getId()))
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.LAST_ADMIN_CANNOT_BE_REMOVED)
                    .verify();
        }

        @Test
        @DisplayName("마지막 ADMIN 역할 변경 시 에러 발생")
        void updateMemberRole_LastAdminDowngrade_Prevented() {
            UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(
                    WorkspaceRole.MEMBER);

            StepVerifier.create(workspaceService.updateMemberRole(
                    testWorkspace.getId(), adminMember.getId(), request,
                    adminUser.getId()))
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.LAST_ADMIN_CANNOT_CHANGE_ROLE)
                    .verify();
        }

        @Test
        @DisplayName("멤버 30명 초과 시 추가 실패")
        void addMember_LimitExceeded_Rejected() {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            // 28명 추가 (현재 2명 + 28명 = 30명)
            Flux.range(0, 28)
                    .flatMap(i -> User.signUp(
                            new UserInfo("extra" + i + "@test.com",
                                    "Extra " + i, "password"),
                            encoder)
                            .flatMap(userRepository::save)
                            .flatMap(user -> workspaceMemberRepository.save(
                                    WorkspaceMember.create(
                                            testWorkspace.getId(),
                                            user.getId(),
                                            WorkspaceRole.MEMBER))),
                            10)
                    .then()
                    .block();

            Long count = workspaceMemberRepository
                    .countByWorkspaceIdAndNotDeleted(testWorkspace.getId())
                    .block();
            assertThat(count).isEqualTo(30L);

            User newUser = User.signUp(
                    new UserInfo("new@test.com", "New User", "password"),
                    encoder).flatMap(userRepository::save).block();

            AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest(
                    newUser.getId(), WorkspaceRole.MEMBER);

            StepVerifier.create(workspaceService.addMember(
                    testWorkspace.getId(), request, adminUser.getId()))
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.MEMBER_LIMIT_EXCEEDED)
                    .verify();
        }

        @Test
        @DisplayName("일반 멤버 제거 성공")
        void removeMember_NormalMember_Success() {
            Mono<Void> result = workspaceService.removeMember(
                    testWorkspace.getId(), normalMember.getId(),
                    adminUser.getId());

            StepVerifier.create(result).verifyComplete();

            WorkspaceMember deleted = workspaceMemberRepository
                    .findById(normalMember.getId()).block();
            assertThat(deleted.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("역할 변경 성공 (멀티 ADMIN 상황)")
        void updateMemberRole_MultipleAdmins_Success() {
            // 기존 normalMember를 ADMIN으로 승격
            normalMember.updateRole(WorkspaceRole.ADMIN);
            normalMember = workspaceMemberRepository.save(normalMember).block();

            // 이제 ADMIN이 2명이므로 역할 변경 가능
            UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(
                    WorkspaceRole.MEMBER);

            Mono<WorkspaceMemberResponse> result = workspaceService
                    .updateMemberRole(testWorkspace.getId(),
                            normalMember.getId(), request, adminUser.getId());

            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.role())
                            .isEqualTo(WorkspaceRole.MEMBER.getValue()))
                    .verifyComplete();
        }

    }

    @Nested
    @DisplayName("멤버 탈퇴")
    class LeaveMember {

        @Test
        @DisplayName("마지막 멤버 탈퇴 시 워크스페이스 삭제")
        void leaveMember_LastMember_DeletesWorkspace() {
            // 새 워크스페이스 생성 (테스트 격리)
            Workspace singleMemberWorkspace = Workspace.create(
                    memberUser.getId(),
                    "Single Member Workspace",
                    "Description",
                    WorkspaceSettings.defaultSettings());
            singleMemberWorkspace = workspaceRepository.save(
                    singleMemberWorkspace).block();

            WorkspaceMember singleMember = WorkspaceMember.create(
                    singleMemberWorkspace.getId(), memberUser.getId(),
                    WorkspaceRole.ADMIN);
            workspaceMemberRepository.save(singleMember).block();

            String workspaceId = singleMemberWorkspace.getId();

            Mono<Void> result = workspaceService.leaveMember(
                    workspaceId, memberUser.getId());

            StepVerifier.create(result).verifyComplete();

            Workspace deleted = workspaceRepository.findById(workspaceId)
                    .block();
            assertThat(deleted.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("마지막 ADMIN 탈퇴 시 에러 발생 (다른 멤버 있음)")
        void leaveMember_LastAdmin_Prevented() {
            StepVerifier.create(workspaceService.leaveMember(
                    testWorkspace.getId(), adminUser.getId()))
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.LAST_ADMIN_CANNOT_LEAVE)
                    .verify();
        }

        @Test
        @DisplayName("일반 멤버 탈퇴 성공")
        void leaveMember_NormalMember_Success() {
            Mono<Void> result = workspaceService.leaveMember(
                    testWorkspace.getId(), memberUser.getId());

            StepVerifier.create(result).verifyComplete();

            WorkspaceMember deleted = workspaceMemberRepository
                    .findById(normalMember.getId()).block();
            assertThat(deleted.isDeleted()).isTrue();

            Workspace workspace = workspaceRepository
                    .findByIdAndNotDeleted(testWorkspace.getId()).block();
            assertThat(workspace).isNotNull();
            assertThat(workspace.isDeleted()).isFalse();
        }

    }

}
