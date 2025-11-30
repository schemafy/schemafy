package com.schemafy.core.workspace.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;
import com.schemafy.core.workspace.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.workspace.controller.dto.request.UpdateWorkspaceRequest;
import com.schemafy.core.workspace.repository.WorkspaceMemberRepository;
import com.schemafy.core.workspace.repository.WorkspaceRepository;
import com.schemafy.core.workspace.repository.entity.Workspace;
import com.schemafy.core.workspace.repository.entity.WorkspaceMember;
import com.schemafy.core.workspace.repository.vo.WorkspaceRole;
import com.schemafy.core.workspace.repository.vo.WorkspaceSettings;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@DisplayName("WorkspaceController 통합 테스트")
class WorkspaceControllerTest {
    private static final String API_BASE_PATH = ApiPath.API
            .replace("{version}", "v1.0") + "/workspaces";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private String testUserId;
    private String testUser2Id;
    private String accessToken;
    private String accessToken2;

    @BeforeEach
    void setUp() {
        Mono<Void> cleanup = Mono.when(workspaceMemberRepository.deleteAll(),
                workspaceRepository.deleteAll(), userRepository.deleteAll());

        Mono<Tuple2<User, User>> createUsers = Mono.zip(User.signUp(
                new UserInfo("test@example.com", "Test User", "password"),
                new BCryptPasswordEncoder()).flatMap(userRepository::save),
                User.signUp(new UserInfo("test2@example.com", "Test User 2",
                        "password"), new BCryptPasswordEncoder())
                        .flatMap(userRepository::save));

        // 3) 체인으로 묶고, 딱 한 번만 block
        Tuple2<User, User> users = cleanup.then(createUsers).blockOptional()
                .orElseThrow(); // null 방지

        User testUser = users.getT1();
        User testUser2 = users.getT2();

        testUserId = testUser.getId();
        testUser2Id = testUser2.getId();

        accessToken = generateAccessToken(testUserId);
        accessToken2 = generateAccessToken(testUser2Id);
    }

    private String generateAccessToken(String userId) {
        return jwtProvider.generateAccessToken(userId, new HashMap<>(),
                System.currentTimeMillis());
    }

    @Test
    @DisplayName("워크스페이스 생성에 성공한다")
    void createWorkspaceSuccess() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                "My Workspace", "Test Description",
                WorkspaceSettings.defaultSettings());

        webTestClient.post().uri(API_BASE_PATH)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isCreated().expectBody()
                .jsonPath("$.success").isEqualTo(true).jsonPath("$.result.name")
                .isEqualTo("My Workspace").jsonPath("$.result.ownerId")
                .isEqualTo(testUserId);

        workspaceMemberRepository.findByUserIdAndNotDeleted(testUserId)
                .collectList().block().forEach(member -> {
                    assertThat(member.getRole()).isEqualTo("admin");
                });
    }

    @Test
    @DisplayName("워크스페이스 생성 시 이름이 없으면 실패한다")
    void createWorkspaceFailWithoutName() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("", null,
                null);

        webTestClient.post().uri(API_BASE_PATH)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("워크스페이스 목록 조회에 성공한다")
    void getWorkspacesSuccess() {
        // given
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();

        WorkspaceMember member = WorkspaceMember.create(workspace.getId(),
                testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member).block();

        // when & then
        webTestClient.get().uri(API_BASE_PATH + "?page=0&size=10")
                .header("Authorization", "Bearer " + accessToken).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.success")
                .isEqualTo(true).jsonPath("$.result.content[0].name")
                .isEqualTo("Test Workspace").jsonPath("$.result.totalElements")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("워크스페이스 상세 조회에 성공한다")
    void getWorkspaceSuccess() {
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();

        WorkspaceMember member = WorkspaceMember.create(workspace.getId(),
                testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member).block();

        webTestClient.get().uri(API_BASE_PATH + "/" + workspace.getId())
                .header("Authorization", "Bearer " + accessToken).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.success")
                .isEqualTo(true).jsonPath("$.result.id")
                .isEqualTo(workspace.getId()).jsonPath("$.result.name")
                .isEqualTo("Test Workspace");
    }

    @Test
    @DisplayName("멤버가 아닌 사용자는 워크스페이스 조회에 실패한다")
    void getWorkspaceFailWhenNotMember() {
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();

        WorkspaceMember member = WorkspaceMember.create(workspace.getId(),
                testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member).block();

        webTestClient.get().uri(API_BASE_PATH + "/" + workspace.getId())
                .header("Authorization", "Bearer " + accessToken2).exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("워크스페이스 수정에 성공한다")
    void updateWorkspaceSuccess() {
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();

        WorkspaceMember member = WorkspaceMember.create(workspace.getId(),
                testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member).block();

        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest(
                "Updated Workspace", "Updated Description",
                new WorkspaceSettings("dark", "en", "editor"));

        webTestClient.put().uri(API_BASE_PATH + "/" + workspace.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.success").isEqualTo(true).jsonPath("$.result.name")
                .isEqualTo("Updated Workspace")
                .jsonPath("$.result.settings.theme").isEqualTo("dark");
    }

    @Test
    @DisplayName("Owner가 아닌 사용자는 워크스페이스 수정에 실패한다")
    void updateWorkspaceFailWhenNotOwner() {
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();

        WorkspaceMember member1 = WorkspaceMember.create(workspace.getId(),
                testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member1).block();

        WorkspaceMember member2 = WorkspaceMember.create(workspace.getId(),
                testUser2Id, WorkspaceRole.MEMBER);
        workspaceMemberRepository.save(member2).block();

        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest(
                "Updated Workspace", "Updated Description", null);

        webTestClient.put().uri(API_BASE_PATH + "/" + workspace.getId())
                .header("Authorization", "Bearer " + accessToken2)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("워크스페이스 삭제에 성공한다")
    void deleteWorkspaceSuccess() {
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();

        WorkspaceMember member = WorkspaceMember.create(workspace.getId(),
                testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member).block();

        webTestClient.delete().uri(API_BASE_PATH + "/" + workspace.getId())
                .header("Authorization", "Bearer " + accessToken).exchange()
                .expectStatus().isNoContent();

        // Verify soft delete
        Workspace deletedWorkspace = workspaceRepository.findById(
                workspace.getId()).block();
        assertThat(deletedWorkspace.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Owner가 아닌 사용자는 워크스페이스 삭제에 실패한다")
    void deleteWorkspaceFailWhenNotOwner() {
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();

        WorkspaceMember member1 = WorkspaceMember.create(workspace.getId(),
                testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member1).block();

        WorkspaceMember member2 = WorkspaceMember.create(workspace.getId(),
                testUser2Id, WorkspaceRole.MEMBER);
        workspaceMemberRepository.save(member2).block();

        webTestClient.delete().uri(API_BASE_PATH + "/" + workspace.getId())
                .header("Authorization", "Bearer " + accessToken2).exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("멤버 목록 조회에 성공한다")
    void getMembersSuccess() {
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();

        WorkspaceMember member = WorkspaceMember.create(workspace.getId(),
                testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member).block();

        webTestClient.get().uri(API_BASE_PATH + "/" + workspace.getId()
                + "/members?page=0&size=20")
                .header("Authorization", "Bearer " + accessToken).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.success")
                .isEqualTo(true).jsonPath("$.result.totalElements")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Admin이 아닌 사용자는 멤버 목록 조회에 실패한다")
    void getMembersFailWhenNotAdmin() {
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();

        WorkspaceMember member1 = WorkspaceMember.create(workspace.getId(),
                testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member1).block();

        WorkspaceMember member2 = WorkspaceMember.create(workspace.getId(),
                testUser2Id, WorkspaceRole.MEMBER);
        workspaceMemberRepository.save(member2).block();

        webTestClient.get().uri(API_BASE_PATH + "/" + workspace.getId()
                + "/members?page=0&size=20")
                .header("Authorization", "Bearer " + accessToken2).exchange()
                .expectStatus().isForbidden();
    }
}
