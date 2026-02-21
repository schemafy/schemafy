package com.schemafy.core.project.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
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
import com.schemafy.core.project.controller.dto.request.AddWorkspaceMemberRequest;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.request.UpdateWorkspaceRequest;
import com.schemafy.core.project.docs.WorkspaceApiSnippets;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
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
        .consumeWith(document("workspace-create",
            WorkspaceApiSnippets.createWorkspaceRequestHeaders(),
            WorkspaceApiSnippets.createWorkspaceRequest(),
            WorkspaceApiSnippets.createWorkspaceResponseHeaders(),
            WorkspaceApiSnippets.createWorkspaceResponse()))
        .jsonPath("$.name").isEqualTo("My Workspace");

    workspaceMemberRepository.findByUserIdAndNotDeleted(testUserId)
        .collectList().block().forEach(member -> {
          assertThat(member.getRole())
              .isEqualTo(WorkspaceRole.ADMIN.getValue());
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
        .expectStatus().isOk().expectBody()
        .consumeWith(document("workspace-list",
            WorkspaceApiSnippets.getWorkspacesRequestHeaders(),
            WorkspaceApiSnippets.getWorkspacesQueryParameters(),
            WorkspaceApiSnippets.getWorkspacesResponseHeaders(),
            WorkspaceApiSnippets.getWorkspacesResponse()))
        .jsonPath("$.content[0].name")
        .isEqualTo("Test Workspace").jsonPath("$.totalElements")
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

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{id}", workspace.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("workspace-get",
            WorkspaceApiSnippets.getWorkspacePathParameters(),
            WorkspaceApiSnippets.getWorkspaceRequestHeaders(),
            WorkspaceApiSnippets.getWorkspaceResponseHeaders(),
            WorkspaceApiSnippets.getWorkspaceResponse()))
        .jsonPath("$.id")
        .isEqualTo(workspace.getId()).jsonPath("$.name")
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
        new WorkspaceSettings("en"));

    webTestClient.put()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{id}", workspace.getId())
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isOk().expectBody()
        .consumeWith(document("workspace-update",
            WorkspaceApiSnippets.updateWorkspacePathParameters(),
            WorkspaceApiSnippets.updateWorkspaceRequestHeaders(),
            WorkspaceApiSnippets.updateWorkspaceRequest(),
            WorkspaceApiSnippets.updateWorkspaceResponseHeaders(),
            WorkspaceApiSnippets.updateWorkspaceResponse()))
        .jsonPath("$.name").isEqualTo("Updated Workspace");
  }

  @Test
  @DisplayName("Admin이 아닌 사용자는 워크스페이스 수정에 실패한다")
  void updateWorkspaceFailWhenNotAdmin() {
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

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{id}", workspace.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent()
        .expectBody()
        .consumeWith(document("workspace-delete",
            WorkspaceApiSnippets.deleteWorkspacePathParameters(),
            WorkspaceApiSnippets.deleteWorkspaceRequestHeaders()));

    Workspace deletedWorkspace = workspaceRepository.findById(
        workspace.getId()).block();
    assertThat(deletedWorkspace.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("Admin이 아닌 사용자는 워크스페이스 삭제에 실패한다")
  void deleteWorkspaceFailWhenNotAdmin() {
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

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{id}/members?page=0&size=10",
            workspace.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("workspace-members",
            WorkspaceApiSnippets
                .getWorkspaceMembersPathParameters(),
            WorkspaceApiSnippets
                .getWorkspaceMembersRequestHeaders(),
            WorkspaceApiSnippets
                .getWorkspaceMembersQueryParameters(),
            WorkspaceApiSnippets
                .getWorkspaceMembersResponseHeaders(),
            WorkspaceApiSnippets.getWorkspaceMembersResponse()))
        .jsonPath("$.totalElements")
        .isEqualTo(1);
  }

  @Test
  @DisplayName("MEMBER도 멤버 목록을 조회할 수 있다")
  void getMembersSuccessWithMemberRole() {
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
        .expectStatus().isOk();
  }

  @Test
  @DisplayName("멤버 추가에 성공한다")
  void addMemberSuccess() {
    Workspace workspace = Workspace.create(testUserId, "Test Workspace",
        "Description", WorkspaceSettings.defaultSettings());
    workspace = workspaceRepository.save(workspace).block();

    WorkspaceMember member = WorkspaceMember.create(workspace.getId(),
        testUserId, WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(member).block();

    AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest(
        testUser2Id, WorkspaceRole.MEMBER);

    webTestClient.post()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members",
            workspace.getId())
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isCreated().expectBody()
        .consumeWith(document("workspace-member-add",
            WorkspaceApiSnippets.addMemberPathParameters(),
            WorkspaceApiSnippets.addMemberRequestHeaders(),
            WorkspaceApiSnippets.addMemberRequest(),
            WorkspaceApiSnippets.addMemberResponseHeaders(),
            WorkspaceApiSnippets.addMemberResponse()))
        .jsonPath("$.userId").isEqualTo(testUser2Id)
        .jsonPath("$.role")
        .isEqualTo(WorkspaceRole.MEMBER.getValue());
  }

  @Test
  @DisplayName("Admin이 아닌 사용자는 멤버 추가에 실패한다")
  void addMemberFailWhenNotAdmin() {
    Workspace workspace = Workspace.create(testUserId, "Test Workspace",
        "Description", WorkspaceSettings.defaultSettings());
    workspace = workspaceRepository.save(workspace).block();

    WorkspaceMember member1 = WorkspaceMember.create(workspace.getId(),
        testUserId, WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(member1).block();

    WorkspaceMember member2 = WorkspaceMember.create(workspace.getId(),
        testUser2Id, WorkspaceRole.MEMBER);
    workspaceMemberRepository.save(member2).block();

    AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest(
        "some-other-user-id", WorkspaceRole.MEMBER);

    webTestClient.post()
        .uri(API_BASE_PATH + "/" + workspace.getId() + "/members")
        .header("Authorization", "Bearer " + accessToken2)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isForbidden();
  }

  @Test
  @DisplayName("멤버 추방에 성공한다")
  void removeMemberSuccess() {
    Workspace workspace = Workspace.create(testUserId, "Test Workspace",
        "Description", WorkspaceSettings.defaultSettings());
    workspace = workspaceRepository.save(workspace).block();

    WorkspaceMember member1 = WorkspaceMember.create(workspace.getId(),
        testUserId, WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(member1).block();

    WorkspaceMember member2 = WorkspaceMember.create(workspace.getId(),
        testUser2Id, WorkspaceRole.MEMBER);
    member2 = workspaceMemberRepository.save(member2).block();

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members/{memberId}",
            workspace.getId(), member2.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent().expectBody()
        .consumeWith(document("workspace-member-remove",
            WorkspaceApiSnippets.removeMemberPathParameters(),
            WorkspaceApiSnippets.removeMemberRequestHeaders()));

    assertThat(workspaceMemberRepository.findById(member2.getId()).block()
        .isDeleted()).isTrue();
  }

  @Test
  @DisplayName("Admin이 아닌 사용자는 멤버 추방에 실패한다")
  void removeMemberFailWhenNotAdmin() {
    Workspace workspace = Workspace.create(testUserId, "Test Workspace",
        "Description", WorkspaceSettings.defaultSettings());
    workspace = workspaceRepository.save(workspace).block();

    WorkspaceMember member1 = WorkspaceMember.create(workspace.getId(),
        testUserId, WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(member1).block();

    WorkspaceMember member2 = WorkspaceMember.create(workspace.getId(),
        testUser2Id, WorkspaceRole.MEMBER);
    member2 = workspaceMemberRepository.save(member2).block();

    webTestClient.delete()
        .uri(API_BASE_PATH + "/" + workspace.getId() + "/members/"
            + member2.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("워크스페이스 탈퇴에 성공한다")
  void leaveMemberSuccess() {
    Workspace workspace = Workspace.create(testUserId, "Test Workspace",
        "Description", WorkspaceSettings.defaultSettings());
    workspace = workspaceRepository.save(workspace).block();

    WorkspaceMember member1 = WorkspaceMember.create(workspace.getId(),
        testUserId, WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(member1).block();

    WorkspaceMember member2 = WorkspaceMember.create(workspace.getId(),
        testUser2Id, WorkspaceRole.MEMBER);
    member2 = workspaceMemberRepository.save(member2).block();

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members/me",
            workspace.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isNoContent().expectBody()
        .consumeWith(document("workspace-member-leave",
            WorkspaceApiSnippets.leaveMemberPathParameters(),
            WorkspaceApiSnippets.leaveMemberRequestHeaders()));

    assertThat(workspaceMemberRepository.findById(member2.getId()).block()
        .isDeleted()).isTrue();
  }

}
