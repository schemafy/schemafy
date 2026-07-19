package com.schemafy.core.project.application.service;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.LeaveProjectCommand;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 탈퇴 서비스 테스트")
class LeaveProjectServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String USER_ID = "user-id";

  @Mock
  ProjectAccessHelper projectAccessHelper;

  @Mock
  ProjectMutationGuard projectMutationGuard;

  @InjectMocks
  LeaveProjectService sut;

  @BeforeEach
  void setUp() {
    given(projectMutationGuard.<Void>protectWorkspaceAndProjectMutation(
        any(String.class), any()))
        .willAnswer(invocation -> invocation
            .<Supplier<Mono<Void>>>getArgument(1).get());
  }

  @Test
  @DisplayName("프로젝트 멤버가 탈퇴하면 멤버십만 삭제한다")
  void leaveProject_softDeletesMembershipOnly() {
    LeaveProjectCommand command = new LeaveProjectCommand(PROJECT_ID, USER_ID);
    ProjectMember member = ProjectMember.create("member-id", PROJECT_ID, USER_ID, ProjectRole.VIEWER);

    given(projectAccessHelper.findProjectMember(USER_ID, PROJECT_ID))
        .willReturn(Mono.just(member));
    given(projectAccessHelper.validateWorkspaceAdminGuard(PROJECT_ID, member))
        .willReturn(Mono.empty());
    given(projectAccessHelper.softDeleteMember(member))
        .willReturn(Mono.empty());
    StepVerifier.create(sut.leaveProject(command))
        .verifyComplete();

    then(projectAccessHelper).should()
        .softDeleteMember(member);
  }

}
