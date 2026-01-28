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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceInvitationRequest;
import com.schemafy.core.project.docs.WorkspaceInvitationApiSnippets;
import com.schemafy.core.project.repository.InvitationRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Invitation;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.InvitationStatus;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
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
@DisplayName("WorkspaceInvitationController 통합 테스트")
class WorkspaceInvitationControllerTest {

  private static final String API_BASE = ApiPath.API.replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository memberRepository;

  @Autowired
  private InvitationRepository invitationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtProvider jwtProvider;

  private String adminUserId;
  private String memberUserId;
  private String invitedUserId;
  private String adminToken;
  private String memberToken;
  private String invitedToken;
  private Workspace testWorkspace;

  @BeforeEach
  void setUp() {
    Mono<Void> cleanup = Mono.when(
        invitationRepository.deleteAll(),
        memberRepository.deleteAll(),
        workspaceRepository.deleteAll(),
        userRepository.deleteAll());

    Mono<Tuple2<User, Tuple2<User, User>>> createUsers = Mono.zip(
        User.signUp(new UserInfo("admin@test.com", "Admin", "password"),
            new BCryptPasswordEncoder()).flatMap(userRepository::save),
        Mono.zip(
            User.signUp(new UserInfo("member@test.com", "Member", "password"),
                new BCryptPasswordEncoder()).flatMap(userRepository::save),
            User.signUp(new UserInfo("invited@test.com", "Invited", "password"),
                new BCryptPasswordEncoder()).flatMap(userRepository::save)));

    var users = cleanup.then(createUsers).blockOptional().orElseThrow();
    User admin = users.getT1();
    User member = users.getT2().getT1();
    User invited = users.getT2().getT2();

    adminUserId = admin.getId();
    memberUserId = member.getId();
    invitedUserId = invited.getId();

    adminToken = generateToken(adminUserId);
    memberToken = generateToken(memberUserId);
    invitedToken = generateToken(invitedUserId);

    testWorkspace = Workspace.create("Test Workspace", "Test Description");
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    WorkspaceMember adminMember = WorkspaceMember.create(
        testWorkspace.getId(), adminUserId, WorkspaceRole.ADMIN);
    memberRepository.save(adminMember).block();

    WorkspaceMember normalMember = WorkspaceMember.create(
        testWorkspace.getId(), memberUserId, WorkspaceRole.MEMBER);
    memberRepository.save(normalMember).block();
  }

  private String generateToken(String userId) {
    return jwtProvider.generateAccessToken(userId, new HashMap<>(),
        System.currentTimeMillis());
  }

  @Nested
  @DisplayName("초대 생성")
  class CreateInvitationTests {

    @Test
    @DisplayName("관리자가 새로운 사용자를 초대하면 201 Created를 반환한다")
    void createInvitation_Success() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated()
          .expectBody()
          .consumeWith(document("workspace-invitation-create",
              WorkspaceInvitationApiSnippets.createInvitationRequestHeaders(),
              WorkspaceInvitationApiSnippets.createInvitationPathParameters(),
              WorkspaceInvitationApiSnippets.createInvitationRequest(),
              WorkspaceInvitationApiSnippets.createInvitationResponseHeaders(),
              WorkspaceInvitationApiSnippets.createInvitationResponse()))
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.workspaceId").isEqualTo(testWorkspace.getId())
          .jsonPath("$.result.invitedEmail").isEqualTo("newuser@test.com")
          .jsonPath("$.result.invitedRole").isEqualTo(WorkspaceRole.MEMBER.getValue())
          .jsonPath("$.result.status").isEqualTo(InvitationStatus.PENDING.getValue());
    }

    @Test
    @DisplayName("일반 멤버가 초대하면 403 Forbidden을 반환한다")
    void createInvitation_NotAdmin_Forbidden() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + memberToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식이면 400 Bad Request를 반환한다")
    void createInvitation_InvalidEmail_BadRequest() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("invalid-email",
          WorkspaceRole.MEMBER);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("인증 토큰 없이 요청하면 401 Unauthorized를 반환한다")
    void createInvitation_NoAuth_Unauthorized() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("중복 초대 시 409 Conflict를 반환한다")
    void createInvitation_Duplicate_Conflict() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated();

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

  @Nested
  @DisplayName("초대 목록 조회")
  class ListInvitationsTests {

    @Test
    @DisplayName("관리자가 초대 목록을 조회하면 200 OK를 반환한다")
    void listInvitations_Success() {
      Invitation invitation1 = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), "user1@test.com", WorkspaceRole.MEMBER, adminUserId);
      Invitation invitation2 = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), "user2@test.com", WorkspaceRole.MEMBER, adminUserId);
      invitationRepository.save(invitation1).block();
      invitationRepository.save(invitation2).block();

      webTestClient.get()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations?page=0&size=10",
              testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("workspace-invitation-list",
              WorkspaceInvitationApiSnippets.listInvitationsRequestHeaders(),
              WorkspaceInvitationApiSnippets.listInvitationsPathParameters(),
              WorkspaceInvitationApiSnippets.listInvitationsQueryParameters(),
              WorkspaceInvitationApiSnippets.listInvitationsResponseHeaders(),
              WorkspaceInvitationApiSnippets.listInvitationsResponse()))
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.content.length()").isEqualTo(2)
          .jsonPath("$.result.totalElements").isEqualTo(2)
          .jsonPath("$.result.page").isEqualTo(0)
          .jsonPath("$.result.size").isEqualTo(10);
    }

    @Test
    @DisplayName("일반 멤버가 목록 조회하면 403 Forbidden을 반환한다")
    void listInvitations_NotAdmin_Forbidden() {
      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/workspaces/{workspaceId}/invitations")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build(testWorkspace.getId()))
          .header("Authorization", "Bearer " + memberToken)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 올바르게 동작한다")
    void listInvitations_Pagination() {
      for (int i = 0; i < 5; i++) {
        Invitation invitation = Invitation.createWorkspaceInvitation(
            testWorkspace.getId(),
            "user" + i + "@test.com",
            WorkspaceRole.MEMBER,
            adminUserId);
        invitationRepository.save(invitation).block();
      }

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/workspaces/{workspaceId}/invitations")
              .queryParam("page", 0)
              .queryParam("size", 2)
              .build(testWorkspace.getId()))
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.result.content.length()").isEqualTo(2)
          .jsonPath("$.result.totalElements").isEqualTo(5);
    }

  }

  @Nested
  @DisplayName("내 초대 목록 조회")
  class ListMyInvitationsTests {

    @Test
    @DisplayName("인증된 사용자가 초대 목록을 조회하면 200 OK를 반환한다")
    void listMyInvitations_Success() {
      User invited = userRepository.findById(invitedUserId).block();

      // 여러 워크스페이스에서 초대 생성
      Workspace workspace2 = Workspace.create("Workspace 2", "Description 2");
      workspace2 = workspaceRepository.save(workspace2).block();

      Invitation invitation1 = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invited.getEmail(), WorkspaceRole.MEMBER, adminUserId);
      Invitation invitation2 = Invitation.createWorkspaceInvitation(
          workspace2.getId(), invited.getEmail(), WorkspaceRole.ADMIN, adminUserId);
      invitationRepository.save(invitation1).block();
      invitationRepository.save(invitation2).block();

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("workspace-invitation-list-my",
              WorkspaceInvitationApiSnippets.listMyInvitationsRequestHeaders(),
              WorkspaceInvitationApiSnippets.listMyInvitationsQueryParameters(),
              WorkspaceInvitationApiSnippets.listMyInvitationsResponseHeaders(),
              WorkspaceInvitationApiSnippets.listMyInvitationsResponse()))
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.content.length()").isEqualTo(2)
          .jsonPath("$.result.totalElements").isEqualTo(2)
          .jsonPath("$.result.content[0].invitedEmail").isEqualTo(invited.getEmail())
          .jsonPath("$.result.content[1].invitedEmail").isEqualTo(invited.getEmail());
    }

    @Test
    @DisplayName("초대가 없으면 빈 목록을 반환한다")
    void listMyInvitations_Empty() {
      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.content.length()").isEqualTo(0)
          .jsonPath("$.result.totalElements").isEqualTo(0);
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 올바르게 동작한다")
    void listMyInvitations_Pagination() {
      User invited = userRepository.findById(invitedUserId).block();

      for (int i = 0; i < 5; i++) {
        Workspace workspace = Workspace.create("Workspace " + i, "Description " + i);
        workspace = workspaceRepository.save(workspace).block();
        Invitation invitation = Invitation.createWorkspaceInvitation(
            workspace.getId(), invited.getEmail(), WorkspaceRole.MEMBER, adminUserId);
        invitationRepository.save(invitation).block();
      }

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 2)
              .build())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.result.content.length()").isEqualTo(2)
          .jsonPath("$.result.totalElements").isEqualTo(5);
    }

    @Test
    @DisplayName("PENDING 상태의 초대만 조회된다")
    void listMyInvitations_OnlyPending() {
      User invited = userRepository.findById(invitedUserId).block();

      // PENDING 초대
      Invitation pending = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invited.getEmail(), WorkspaceRole.MEMBER, adminUserId);
      pending = invitationRepository.save(pending).block();

      // ACCEPTED 초대
      Workspace workspace2 = Workspace.create("Workspace 2", "Description 2");
      workspace2 = workspaceRepository.save(workspace2).block();
      Invitation accepted = Invitation.createWorkspaceInvitation(
          workspace2.getId(), invited.getEmail(), WorkspaceRole.MEMBER, adminUserId);
      accepted = invitationRepository.save(accepted).block();
      accepted.accept();
      invitationRepository.save(accepted).block();

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.result.content.length()").isEqualTo(1)
          .jsonPath("$.result.content[0].id").isEqualTo(pending.getId());
    }

    @Test
    @DisplayName("인증 토큰 없이 요청하면 401 Unauthorized를 반환한다")
    void listMyInvitations_NoAuth_Unauthorized() {
      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build())
          .exchange()
          .expectStatus().isUnauthorized();
    }

  }

  @Nested
  @DisplayName("초대 수락")
  class AcceptInvitationTests {

    @Test
    @DisplayName("초대받은 사용자가 초대를 수락하면 200 OK를 반환하고 멤버가 생성된다")
    void acceptInvitation_Success() {
      User invited = userRepository.findById(invitedUserId).block();
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invited.getEmail(),
          WorkspaceRole.MEMBER,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("workspace-invitation-accept",
              WorkspaceInvitationApiSnippets.acceptInvitationRequestHeaders(),
              WorkspaceInvitationApiSnippets.acceptInvitationPathParameters(),
              WorkspaceInvitationApiSnippets.acceptInvitationResponseHeaders(),
              WorkspaceInvitationApiSnippets.acceptInvitationResponse()))
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.userId").isEqualTo(invitedUserId)
          .jsonPath("$.result.workspaceId").isEqualTo(testWorkspace.getId())
          .jsonPath("$.result.role").isEqualTo(WorkspaceRole.MEMBER.getValue());

      Invitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);

      boolean memberExists = memberRepository
          .existsByWorkspaceIdAndUserIdAndNotDeleted(testWorkspace.getId(), invitedUserId)
          .block();
      assertThat(memberExists).isTrue();
    }

    @Test
    @DisplayName("다른 사용자가 초대를 수락하면 403 Forbidden을 반환한다")
    void acceptInvitation_WrongUser_Forbidden() {
      User invited = userRepository.findById(invitedUserId).block();
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invited.getEmail(),
          WorkspaceRole.MEMBER,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + memberToken)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("존재하지 않는 초대를 수락하면 404 Not Found를 반환한다")
    void acceptInvitation_NotFound() {
      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              "nonexistent-id")
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("이미 수락된 초대를 다시 수락하면 409 Conflict를 반환한다")
    void acceptInvitation_AlreadyAccepted_Conflict() {
      User invited = userRepository.findById(invitedUserId).block();
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invited.getEmail(),
          WorkspaceRole.MEMBER,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

  @Nested
  @DisplayName("초대 거절")
  class RejectInvitationTests {

    @Test
    @DisplayName("초대받은 사용자가 초대를 거절하면 204 No Content를 반환한다")
    void rejectInvitation_Success() {
      User invited = userRepository.findById(invitedUserId).block();
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invited.getEmail(),
          WorkspaceRole.MEMBER,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isNoContent()
          .expectBody()
          .consumeWith(document("workspace-invitation-reject",
              WorkspaceInvitationApiSnippets.rejectInvitationRequestHeaders(),
              WorkspaceInvitationApiSnippets.rejectInvitationPathParameters()));

      Invitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
    }

    @Test
    @DisplayName("다른 사용자가 초대를 거절하면 403 Forbidden을 반환한다")
    void rejectInvitation_WrongUser_Forbidden() {
      User invited = userRepository.findById(invitedUserId).block();
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invited.getEmail(),
          WorkspaceRole.MEMBER,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + memberToken)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("존재하지 않는 초대를 거절하면 404 Not Found를 반환한다")
    void rejectInvitation_NotFound() {
      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              "nonexistent-id")
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("이미 거절된 초대를 다시 거절하면 409 Conflict를 반환한다")
    void rejectInvitation_AlreadyRejected_Conflict() {
      User invited = userRepository.findById(invitedUserId).block();
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invited.getEmail(),
          WorkspaceRole.MEMBER,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isNoContent();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("이미 수락된 초대를 거절하면 409 Conflict를 반환한다")
    void rejectInvitation_AlreadyAccepted_Conflict() {
      User invited = userRepository.findById(invitedUserId).block();
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invited.getEmail(),
          WorkspaceRole.MEMBER,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk();

      webTestClient.put()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

}
