package com.schemafy.core.project.controller;

import java.util.HashMap;

import com.schemafy.core.RestDocsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.project.controller.dto.request.CreateProjectRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectRequest;
import com.schemafy.core.project.docs.ProjectApiSnippets;
import com.schemafy.core.project.repository.ProjectMemberRepository;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.ProjectSettings;
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
@Import(RestDocsConfiguration.class)
@DisplayName("ProjectController 통합 테스트")
class ProjectControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

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
    private String testWorkspaceId;
    private String accessToken;
    private String accessToken2;
    private String apiBasePath;

    @BeforeEach
    void setUp() {
        Mono<Void> cleanup = Mono.when(projectMemberRepository.deleteAll(),
                projectRepository.deleteAll(),
                workspaceMemberRepository.deleteAll(),
                workspaceRepository.deleteAll(), userRepository.deleteAll());

        Mono<Tuple2<User, User>> createUsers = Mono.zip(
                User.signUp(new UserInfo("test@example.com", "Test User",
                        "password"), new BCryptPasswordEncoder())
                        .flatMap(userRepository::save),
                User.signUp(new UserInfo("test2@example.com", "Test User 2",
                        "password"), new BCryptPasswordEncoder())
                        .flatMap(userRepository::save));

        Tuple2<User, User> users = cleanup.then(createUsers).blockOptional()
                .orElseThrow();

        User testUser = users.getT1();
        User testUser2 = users.getT2();

        testUserId = testUser.getId();
        testUser2Id = testUser2.getId();

        accessToken = generateAccessToken(testUserId);
        accessToken2 = generateAccessToken(testUser2Id);

        // Create workspace and add members
        Workspace workspace = Workspace.create(testUserId, "Test Workspace",
                "Description", WorkspaceSettings.defaultSettings());
        workspace = workspaceRepository.save(workspace).block();
        testWorkspaceId = workspace.getId();

        WorkspaceMember member1 = WorkspaceMember
                .create(testWorkspaceId, testUserId, WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(member1).block();

        WorkspaceMember member2 = WorkspaceMember
                .create(testWorkspaceId, testUser2Id, WorkspaceRole.MEMBER);
        workspaceMemberRepository.save(member2).block();

        apiBasePath = ApiPath.API.replace("{version}", "v1.0")
                + "/workspaces/" + testWorkspaceId + "/projects";
    }

    private String generateAccessToken(String userId) {
        return jwtProvider.generateAccessToken(userId, new HashMap<>(),
                System.currentTimeMillis());
    }

    @Test
    @DisplayName("프로젝트 생성에 성공한다")
    void createProjectSuccess() {
        CreateProjectRequest request = new CreateProjectRequest("My Project",
                "Test Description", ProjectSettings.defaultSettings());

        webTestClient.post()
                .uri(ApiPath.API.replace("{version}", "v1.0") + "/workspaces/{workspaceId}/projects", testWorkspaceId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isCreated().expectBody()
                .consumeWith(document("project-create",
                        ProjectApiSnippets.createProjectPathParameters(),
                        ProjectApiSnippets.createProjectRequestHeaders(),
                        ProjectApiSnippets.createProjectRequest(),
                        ProjectApiSnippets.createProjectResponseHeaders(),
                        ProjectApiSnippets.createProjectResponse()))
                .jsonPath("$.success").isEqualTo(true).jsonPath("$.result.name")
                .isEqualTo("My Project").jsonPath("$.result.workspaceId")
                .isEqualTo(testWorkspaceId).jsonPath("$.result.ownerId")
                .isEqualTo(testUserId);

        projectMemberRepository.findByUserIdAndNotDeleted(testUserId)
                .collectList().block().forEach(member -> {
                    assertThat(member.getRole()).isEqualTo("owner");
                });
    }

    @Test
    @DisplayName("프로젝트 생성 시 이름이 없으면 실패한다")
    void createProjectFailWithoutName() {
        CreateProjectRequest request = new CreateProjectRequest("", null, null);

        webTestClient.post().uri(apiBasePath)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("워크스페이스 멤버가 아닌 사용자는 프로젝트를 생성할 수 없다")
    void createProjectFailWhenNotWorkspaceMember() {
        // Remove user2 from workspace
        workspaceMemberRepository.findByUserIdAndNotDeleted(testUser2Id)
                .flatMap(workspaceMemberRepository::delete).blockLast();

        CreateProjectRequest request = new CreateProjectRequest("My Project",
                "Description", null);

        webTestClient.post().uri(apiBasePath)
                .header("Authorization", "Bearer " + accessToken2)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("프로젝트 목록 조회에 성공한다")
    void getProjectsSuccess() {
        Project project = Project.create(testWorkspaceId, testUserId,
                "Test Project", "Description",
                ProjectSettings.defaultSettings());
        project = projectRepository.save(project).block();

        ProjectMember member = ProjectMember.create(project.getId(),
                testUserId, ProjectRole.OWNER);
        projectMemberRepository.save(member).block();

        webTestClient.get()
                .uri(ApiPath.API.replace("{version}", "v1.0") + "/workspaces/{workspaceId}/projects?page=0&size=10", testWorkspaceId)
                .header("Authorization", "Bearer " + accessToken).exchange()
                .expectStatus().isOk().expectBody()
                .consumeWith(document("project-list",
                        ProjectApiSnippets.getProjectsPathParameters(),
                        ProjectApiSnippets.getProjectsRequestHeaders(),
                        ProjectApiSnippets.getProjectsQueryParameters(),
                        ProjectApiSnippets.getProjectsResponseHeaders(),
                        ProjectApiSnippets.getProjectsResponse()))
                .jsonPath("$.success")
                .isEqualTo(true).jsonPath("$.result.content[0].name")
                .isEqualTo("Test Project").jsonPath("$.result.totalElements")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("프로젝트 상세 조회에 성공한다")
    void getProjectSuccess() {
        Project project = Project.create(testWorkspaceId, testUserId,
                "Test Project", "Description",
                ProjectSettings.defaultSettings());
        project = projectRepository.save(project).block();

        ProjectMember member = ProjectMember.create(project.getId(),
                testUserId, ProjectRole.OWNER);
        projectMemberRepository.save(member).block();

        webTestClient.get()
                .uri(ApiPath.API.replace("{version}", "v1.0") + "/workspaces/{workspaceId}/projects/{id}", testWorkspaceId, project.getId())
                .header("Authorization", "Bearer " + accessToken).exchange()
                .expectStatus().isOk().expectBody()
                .consumeWith(document("project-get",
                        ProjectApiSnippets.getProjectPathParameters(),
                        ProjectApiSnippets.getProjectRequestHeaders(),
                        ProjectApiSnippets.getProjectResponseHeaders(),
                        ProjectApiSnippets.getProjectResponse()))
                .jsonPath("$.success")
                .isEqualTo(true).jsonPath("$.result.id")
                .isEqualTo(project.getId()).jsonPath("$.result.name")
                .isEqualTo("Test Project");
    }

    @Test
    @DisplayName("멤버가 아닌 사용자는 프로젝트 조회에 실패한다")
    void getProjectFailWhenNotMember() {
        Project project = Project.create(testWorkspaceId, testUserId,
                "Test Project", "Description",
                ProjectSettings.defaultSettings());
        project = projectRepository.save(project).block();

        ProjectMember member = ProjectMember.create(project.getId(),
                testUserId, ProjectRole.OWNER);
        projectMemberRepository.save(member).block();

        webTestClient.get().uri(apiBasePath + "/" + project.getId())
                .header("Authorization", "Bearer " + accessToken2).exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("프로젝트 수정에 성공한다")
    void updateProjectSuccess() {
        Project project = Project.create(testWorkspaceId, testUserId,
                "Test Project", "Description",
                ProjectSettings.defaultSettings());
        project = projectRepository.save(project).block();

        ProjectMember member = ProjectMember.create(project.getId(),
                testUserId, ProjectRole.OWNER);
        projectMemberRepository.save(member).block();

        UpdateProjectRequest request = new UpdateProjectRequest(
                "Updated Project", "Updated Description",
                new ProjectSettings("dark", "en", "board"));

        webTestClient.put()
                .uri(ApiPath.API.replace("{version}", "v1.0") + "/workspaces/{workspaceId}/projects/{id}", testWorkspaceId, project.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isOk().expectBody()
                .consumeWith(document("project-update",
                        ProjectApiSnippets.updateProjectPathParameters(),
                        ProjectApiSnippets.updateProjectRequestHeaders(),
                        ProjectApiSnippets.updateProjectRequest(),
                        ProjectApiSnippets.updateProjectResponseHeaders(),
                        ProjectApiSnippets.updateProjectResponse()))
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("Updated Project")
                .jsonPath("$.result.settings.theme").isEqualTo("dark");
    }

    @Test
    @DisplayName("Admin이 아닌 사용자는 프로젝트 수정에 실패한다")
    void updateProjectFailWhenNotAdmin() {
        Project project = Project.create(testWorkspaceId, testUserId,
                "Test Project", "Description",
                ProjectSettings.defaultSettings());
        project = projectRepository.save(project).block();

        ProjectMember member1 = ProjectMember.create(project.getId(),
                testUserId, ProjectRole.OWNER);
        projectMemberRepository.save(member1).block();

        ProjectMember member2 = ProjectMember.create(project.getId(),
                testUser2Id, ProjectRole.VIEWER);
        projectMemberRepository.save(member2).block();

        UpdateProjectRequest request = new UpdateProjectRequest(
                "Updated Project", "Updated Description", null);

        webTestClient.put().uri(apiBasePath + "/" + project.getId())
                .header("Authorization", "Bearer " + accessToken2)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("프로젝트 삭제에 성공한다")
    void deleteProjectSuccess() {
        Project project = Project.create(testWorkspaceId, testUserId,
                "Test Project", "Description",
                ProjectSettings.defaultSettings());
        project = projectRepository.save(project).block();

        ProjectMember member = ProjectMember.create(project.getId(),
                testUserId, ProjectRole.OWNER);
        projectMemberRepository.save(member).block();

        webTestClient.delete()
                .uri(ApiPath.API.replace("{version}", "v1.0") + "/workspaces/{workspaceId}/projects/{id}", testWorkspaceId, project.getId())
                .header("Authorization", "Bearer " + accessToken).exchange()
                .expectStatus().isNoContent()
                .expectBody()
                .consumeWith(document("project-delete",
                        ProjectApiSnippets.deleteProjectPathParameters(),
                        ProjectApiSnippets.deleteProjectRequestHeaders()));

        // Verify soft delete
        Project deletedProject = projectRepository.findById(project.getId())
                .block();
        assertThat(deletedProject.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Owner가 아닌 사용자는 프로젝트 삭제에 실패한다")
    void deleteProjectFailWhenNotOwner() {
        Project project = Project.create(testWorkspaceId, testUserId,
                "Test Project", "Description",
                ProjectSettings.defaultSettings());
        project = projectRepository.save(project).block();

        ProjectMember member1 = ProjectMember.create(project.getId(),
                testUserId, ProjectRole.OWNER);
        projectMemberRepository.save(member1).block();

        ProjectMember member2 = ProjectMember.create(project.getId(),
                testUser2Id, ProjectRole.ADMIN);
        projectMemberRepository.save(member2).block();

        webTestClient.delete().uri(apiBasePath + "/" + project.getId())
                .header("Authorization", "Bearer " + accessToken2).exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("멤버 목록 조회에 성공한다")
    void getMembersSuccess() {
        Project project = Project.create(testWorkspaceId, testUserId,
                "Test Project", "Description",
                ProjectSettings.defaultSettings());
        project = projectRepository.save(project).block();

        ProjectMember member = ProjectMember.create(project.getId(),
                testUserId, ProjectRole.OWNER);
        projectMemberRepository.save(member).block();

        webTestClient.get()
                .uri(ApiPath.API.replace("{version}", "v1.0") + "/workspaces/{workspaceId}/projects/{id}/members?page=0&size=20", testWorkspaceId, project.getId())
                .header("Authorization", "Bearer " + accessToken).exchange()
                .expectStatus().isOk().expectBody()
                .consumeWith(document("project-members",
                        ProjectApiSnippets.getProjectMembersPathParameters(),
                        ProjectApiSnippets.getProjectMembersRequestHeaders(),
                        ProjectApiSnippets.getProjectMembersQueryParameters(),
                        ProjectApiSnippets.getProjectMembersResponseHeaders(),
                        ProjectApiSnippets.getProjectMembersResponse()))
                .jsonPath("$.success")
                .isEqualTo(true).jsonPath("$.result.totalElements")
                .isEqualTo(1);
    }

}