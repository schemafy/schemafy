package com.schemafy.core.project.service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.project.controller.dto.request.CreateShareLinkRequest;
import com.schemafy.core.project.controller.dto.response.ShareLinkAccessResponse;
import com.schemafy.core.project.controller.dto.response.ShareLinkResponse;
import com.schemafy.core.project.repository.*;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ShareLink;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.ShareLinkRole;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("ShareLinkService 테스트")
class ShareLinkServiceTest {

    @Autowired
    private ShareLinkService shareLinkService;

    @Autowired
    private ShareLinkRepository shareLinkRepository;

    @Autowired
    private ShareLinkAccessLogRepository accessLogRepository;

    @Autowired
    private ShareLinkTokenService tokenService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User testUser2;
    private Workspace testWorkspace;
    private Project testProject;

    @BeforeEach
    void setUp() {
        // Clean up in order of dependencies
        Mono.when(accessLogRepository.deleteAll(),
                shareLinkRepository.deleteAll(),
                projectRepository.deleteAll(),
                workspaceMemberRepository.deleteAll(),
                workspaceRepository.deleteAll(), userRepository.deleteAll())
                .block();

        // Create test users
        testUser = User
                .signUp(new UserInfo("owner@example.com", "Owner",
                        "password"), new BCryptPasswordEncoder())
                .flatMap(userRepository::save).block();

        testUser2 = User
                .signUp(new UserInfo("member@example.com", "Member",
                        "password"), new BCryptPasswordEncoder())
                .flatMap(userRepository::save).block();

        // Create workspace
        testWorkspace = Workspace.create(testUser.getId(), "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        testWorkspace = workspaceRepository.save(testWorkspace).block();

        // Add workspace members
        WorkspaceMember member1 = WorkspaceMember.create(testWorkspace.getId(),
                testUser.getId(), WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member1).block();

        WorkspaceMember member2 = WorkspaceMember.create(testWorkspace.getId(),
                testUser2.getId(), WorkspaceRole.MEMBER);
        workspaceMemberRepository.save(member2).block();

        // Create project owned by testUser
        testProject = Project.create(testWorkspace.getId(), testUser.getId(),
                "Test Project", "Description",
                ProjectSettings.defaultSettings());
        testProject = projectRepository.save(testProject).block();
    }

    @Nested
    @DisplayName("공유 링크 생성")
    class CreateShareLink {

        @Test
        @DisplayName("프로젝트 소유자가 공유 링크를 생성할 수 있다")
        void createShareLink_Success() {
            CreateShareLinkRequest request = new CreateShareLinkRequest(
                    "viewer",
                    null);

            Mono<ShareLinkResponse> result = shareLinkService.createShareLink(
                    testWorkspace.getId(), testProject.getId(), request,
                    testUser.getId());

            StepVerifier.create(result).assertNext(response -> {
                assertThat(response.id()).isNotNull();
                assertThat(response.projectId()).isEqualTo(testProject.getId());
                assertThat(response.role()).isEqualTo("viewer");
                assertThat(response.token()).isNotNull();
                assertThat(response.isRevoked()).isFalse();
            }).verifyComplete();
        }

        @Test
        @DisplayName("만료 시간을 설정하여 공유 링크를 생성할 수 있다")
        void createShareLink_WithExpiration() {
            Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
            CreateShareLinkRequest request = new CreateShareLinkRequest(
                    "editor",
                    expiresAt);

            Mono<ShareLinkResponse> result = shareLinkService.createShareLink(
                    testWorkspace.getId(), testProject.getId(), request,
                    testUser.getId());

            StepVerifier.create(result).assertNext(response -> {
                assertThat(response.role()).isEqualTo("editor");
                assertThat(response.expiresAt()).isNotNull();
            }).verifyComplete();
        }

        @Test
        @DisplayName("프로젝트 소유자가 아닌 사용자는 공유 링크를 생성할 수 없다")
        void createShareLink_FailsWhenNotOwner() {
            CreateShareLinkRequest request = new CreateShareLinkRequest(
                    "viewer",
                    null);

            Mono<ShareLinkResponse> result = shareLinkService.createShareLink(
                    testWorkspace.getId(), testProject.getId(), request,
                    testUser2.getId());

            StepVerifier.create(result)
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.ACCESS_DENIED)
                    .verify();
        }

        @Test
        @DisplayName("워크스페이스 멤버가 아닌 사용자는 공유 링크를 생성할 수 없다")
        void createShareLink_FailsWhenNotWorkspaceMember() {
            // Create a user who is not a workspace member
            User outsider = User
                    .signUp(new UserInfo("outsider@example.com", "Outsider",
                            "password"), new BCryptPasswordEncoder())
                    .flatMap(userRepository::save).block();

            CreateShareLinkRequest request = new CreateShareLinkRequest(
                    "viewer",
                    null);

            Mono<ShareLinkResponse> result = shareLinkService.createShareLink(
                    testWorkspace.getId(), testProject.getId(), request,
                    outsider.getId());

            StepVerifier.create(result)
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.WORKSPACE_ACCESS_DENIED)
                    .verify();
        }

    }

    @Nested
    @DisplayName("공유 링크 조회")
    class GetShareLinks {

        @Test
        @DisplayName("프로젝트의 공유 링크 목록을 조회할 수 있다")
        void getShareLinks_Success() {
            // Create share links
            createTestShareLink(ShareLinkRole.VIEWER, null);
            createTestShareLink(ShareLinkRole.EDITOR, null);

            StepVerifier
                    .create(shareLinkService.getShareLinks(
                            testWorkspace.getId(),
                            testProject.getId(), testUser.getId(), 0, 20))
                    .assertNext(page -> {
                        assertThat(page.totalElements()).isEqualTo(2);
                        assertThat(page.content()).hasSize(2);
                    }).verifyComplete();
        }

        @Test
        @DisplayName("공유 링크 상세 조회에 성공한다")
        void getShareLink_Success() {
            ShareLink shareLink = createTestShareLink(ShareLinkRole.VIEWER,
                    null);

            StepVerifier
                    .create(shareLinkService.getShareLink(testWorkspace.getId(),
                            testProject.getId(), shareLink.getId(),
                            testUser.getId()))
                    .assertNext(response -> {
                        assertThat(response.id()).isEqualTo(shareLink.getId());
                        assertThat(response.role()).isEqualTo("viewer");
                    }).verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 공유 링크 조회는 실패한다")
        void getShareLink_NotFound() {
            StepVerifier
                    .create(shareLinkService.getShareLink(testWorkspace.getId(),
                            testProject.getId(), "nonexistent-id",
                            testUser.getId()))
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.SHARE_LINK_NOT_FOUND)
                    .verify();
        }

    }

    @Nested
    @DisplayName("공유 링크 비활성화")
    class RevokeShareLink {

        @Test
        @DisplayName("공유 링크 비활성화에 성공한다")
        void revokeShareLink_Success() {
            ShareLink shareLink = createTestShareLink(ShareLinkRole.VIEWER,
                    null);

            StepVerifier
                    .create(shareLinkService.revokeShareLink(
                            testWorkspace.getId(),
                            testProject.getId(), shareLink.getId(),
                            testUser.getId()))
                    .verifyComplete();

            // Verify revoked
            ShareLink revoked = shareLinkRepository
                    .findByIdAndNotDeleted(shareLink.getId()).block();
            assertThat(revoked.getIsRevoked()).isTrue();
        }

    }

    @Nested
    @DisplayName("공유 링크 삭제")
    class DeleteShareLink {

        @Test
        @DisplayName("공유 링크 삭제에 성공한다")
        void deleteShareLink_Success() {
            ShareLink shareLink = createTestShareLink(ShareLinkRole.VIEWER,
                    null);

            StepVerifier
                    .create(shareLinkService.deleteShareLink(
                            testWorkspace.getId(),
                            testProject.getId(), shareLink.getId(),
                            testUser.getId()))
                    .verifyComplete();

            // Verify soft deleted
            ShareLink deleted = shareLinkRepository.findById(shareLink.getId())
                    .block();
            assertThat(deleted.isDeleted()).isTrue();
        }

    }

    @Nested
    @DisplayName("토큰으로 접근")
    class AccessByToken {

        @Test
        @DisplayName("유효한 토큰으로 프로젝트에 접근할 수 있다")
        void accessByToken_Success() {
            String token = tokenService.generateToken();
            byte[] tokenHash = tokenService.hashToken(token);
            ShareLink shareLink = ShareLink.create(testProject.getId(),
                    tokenHash,
                    ShareLinkRole.EDITOR, null);
            shareLinkRepository.save(shareLink).block();

            Mono<ShareLinkAccessResponse> result = shareLinkService
                    .accessByToken(token, testUser.getId(), "127.0.0.1",
                            "Test Agent");

            StepVerifier.create(result).assertNext(response -> {
                assertThat(response.projectId()).isEqualTo(testProject.getId());
                assertThat(response.projectName()).isEqualTo("Test Project");
                assertThat(response.grantedRole()).isEqualTo("editor");
                assertThat(response.canEdit()).isTrue();
            }).verifyComplete();
        }

        @Test
        @DisplayName("익명 사용자는 VIEWER 권한만 부여받는다")
        void accessByToken_AnonymousGetsViewer() {
            String token = tokenService.generateToken();
            byte[] tokenHash = tokenService.hashToken(token);
            ShareLink shareLink = ShareLink.create(testProject.getId(),
                    tokenHash,
                    ShareLinkRole.EDITOR, null);
            shareLinkRepository.save(shareLink).block();

            Mono<ShareLinkAccessResponse> result = shareLinkService
                    .accessByToken(token, null, "127.0.0.1", "Test Agent");

            StepVerifier.create(result).assertNext(response -> {
                assertThat(response.grantedRole()).isEqualTo("viewer");
                assertThat(response.canEdit()).isFalse();
                assertThat(response.canComment()).isFalse();
            }).verifyComplete();
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 접근하면 실패한다")
        void accessByToken_InvalidToken() {
            Mono<ShareLinkAccessResponse> result = shareLinkService
                    .accessByToken("invalid-token", null, "127.0.0.1",
                            "Test Agent");

            StepVerifier.create(result)
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.SHARE_LINK_INVALID)
                    .verify();
        }

        @Test
        @DisplayName("비활성화된 공유 링크로 접근하면 실패한다")
        void accessByToken_RevokedLink() {
            String token = tokenService.generateToken();
            byte[] tokenHash = tokenService.hashToken(token);
            ShareLink shareLink = ShareLink.create(testProject.getId(),
                    tokenHash,
                    ShareLinkRole.VIEWER, null);
            shareLink.revoke();
            shareLinkRepository.save(shareLink).block();

            Mono<ShareLinkAccessResponse> result = shareLinkService
                    .accessByToken(token, null, "127.0.0.1", "Test Agent");

            StepVerifier.create(result)
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.SHARE_LINK_INVALID)
                    .verify();
        }

        @Test
        @DisplayName("만료된 공유 링크로 접근하면 실패한다")
        void accessByToken_ExpiredLink() throws Exception {
            String token = tokenService.generateToken();
            byte[] tokenHash = tokenService.hashToken(token);
            // Create with future date first
            ShareLink shareLink = ShareLink.create(testProject.getId(),
                    tokenHash,
                    ShareLinkRole.VIEWER, Instant.now().plus(1, ChronoUnit.DAYS));
            shareLinkRepository.save(shareLink).block();

            // Change expiresAt to past using reflection
            Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
            var field = ShareLink.class.getDeclaredField("expiresAt");
            field.setAccessible(true);
            field.set(shareLink, pastDate);
            shareLinkRepository.save(shareLink).block();

            Mono<ShareLinkAccessResponse> result = shareLinkService
                    .accessByToken(token, null, "127.0.0.1", "Test Agent");

            StepVerifier.create(result)
                    .expectErrorMatches(e -> e instanceof BusinessException &&
                            ((BusinessException) e)
                                    .getErrorCode() == ErrorCode.SHARE_LINK_INVALID)
                    .verify();
        }

    }

    private ShareLink createTestShareLink(ShareLinkRole role,
            Instant expiresAt) {
        String token = tokenService.generateToken();
        byte[] tokenHash = tokenService.hashToken(token);
        ShareLink shareLink = ShareLink.create(testProject.getId(), tokenHash,
                role, expiresAt);
        return shareLinkRepository.save(shareLink).block();
    }

}
