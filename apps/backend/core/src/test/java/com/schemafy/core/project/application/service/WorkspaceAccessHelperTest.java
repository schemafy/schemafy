package com.schemafy.core.project.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크스페이스 관리자 접근 검증")
class WorkspaceAccessHelperTest {

  private static final String WORKSPACE_ID = "workspace-id";
  private static final String USER_ID = "user-id";

  @Mock
  WorkspacePort workspacePort;

  @Mock
  WorkspaceMemberPort workspaceMemberPort;

  @Mock
  ProjectPort projectPort;

  @Mock
  FindUserByEmailPort findUserByEmailPort;

  @InjectMocks
  WorkspaceAccessHelper sut;

  @Test
  @DisplayName("활성 멤버십이 없으면 접근 거부를 반환한다")
  void returnsAccessDeniedWhenActiveMembershipIsMissing() {
    given(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(WORKSPACE_ID, USER_ID))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.findWorkspaceAdminMember(USER_ID, WORKSPACE_ID))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("활성 일반 멤버는 관리자 권한 필요 오류를 반환한다")
  void returnsAdminRequiredWhenActiveMemberIsNotAdmin() {
    var member = WorkspaceMember.create("member-id", WORKSPACE_ID, USER_ID,
        WorkspaceRole.MEMBER);
    given(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(WORKSPACE_ID, USER_ID))
        .willReturn(Mono.just(member));

    StepVerifier.create(sut.findWorkspaceAdminMember(USER_ID, WORKSPACE_ID))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("활성 관리자는 최신 멤버십을 반환한다")
  void returnsActiveAdminMembership() {
    var member = WorkspaceMember.create("member-id", WORKSPACE_ID, USER_ID,
        WorkspaceRole.ADMIN);
    given(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(WORKSPACE_ID, USER_ID))
        .willReturn(Mono.just(member));

    StepVerifier.create(sut.findWorkspaceAdminMember(USER_ID, WORKSPACE_ID))
        .expectNext(member)
        .verifyComplete();
  }

}
