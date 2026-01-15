package com.schemafy.core.project.controller;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.ShareLinkRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ShareLink;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@DisplayName("PublicShareLinkController 복원력 테스트 (Resilience Test)")
class PublicShareLinkControllerResilienceTest {

  private static final String PUBLIC_API_PATH = "/public/api/v1.0/share";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Autowired
  private WebTestClient webTestClient;

  @MockitoSpyBean
  private ShareLinkRepository shareLinkRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository workspaceMemberRepository;

  @Autowired
  private UserRepository userRepository;

  private User testUser;
  private Workspace testWorkspace;
  private Project testProject;

  @BeforeEach
  void setUp() {
    Mono.when(shareLinkRepository.deleteAll(),
        projectRepository.deleteAll(),
        workspaceMemberRepository.deleteAll(),
        workspaceRepository.deleteAll(),
        userRepository.deleteAll())
        .block();

    testUser = User.signUp(
        new UserInfo("owner@example.com", "Owner", "password"),
        new BCryptPasswordEncoder())
        .flatMap(userRepository::save)
        .block();

    testWorkspace = Workspace.create("Test Workspace", "Description");
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    WorkspaceMember member = WorkspaceMember.create(
        testWorkspace.getId(),
        testUser.getId(),
        WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(member).block();

    testProject = Project.create(
        testWorkspace.getId(),
        "Test Project",
        "Description",
        ProjectSettings.defaultSettings());
    testProject = projectRepository.save(testProject).block();
  }

  private String generateLinkCode() {
    byte[] bytes = new byte[24];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private ShareLink createShareLink(String code) {
    ShareLink shareLink = ShareLink.create(testProject.getId(), code, null);
    return shareLinkRepository.save(shareLink).block();
  }

  @Test
  @DisplayName("incrementAccessCount가 실패해도 프로젝트 조회는 성공한다")
  void accessByCode_IncrementAccessCountFailure_StillSucceeds() {
    String code = generateLinkCode();
    createShareLink(code);

    // incrementAccessCount만 실패하도록 설정
    doReturn(Mono.error(new RuntimeException("DB write failed")))
        .when(shareLinkRepository)
        .incrementAccessCount(anyString());

    // 접근은 성공해야 함. accessCount는 증가하지 않았지만, 응답은 성공
    webTestClient.get()
        .uri(PUBLIC_API_PATH + "/{code}", code)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.projectId").isEqualTo(testProject.getId())
        .jsonPath("$.result.projectName").isEqualTo("Test Project");
  }

}
