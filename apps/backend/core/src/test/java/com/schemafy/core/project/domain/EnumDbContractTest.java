package com.schemafy.core.project.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Project enum DB contract")
class EnumDbContractTest {

  @Test
  @DisplayName("ProjectRole name은 DB 저장 값으로 고정된다")
  void projectRoleNames_areFixed() {
    assertThat(ProjectRole.ADMIN.name()).isEqualTo("ADMIN");
    assertThat(ProjectRole.EDITOR.name()).isEqualTo("EDITOR");
    assertThat(ProjectRole.VIEWER.name()).isEqualTo("VIEWER");
  }

  @Test
  @DisplayName("WorkspaceRole name은 DB 저장 값으로 고정된다")
  void workspaceRoleNames_areFixed() {
    assertThat(WorkspaceRole.ADMIN.name()).isEqualTo("ADMIN");
    assertThat(WorkspaceRole.MEMBER.name()).isEqualTo("MEMBER");
  }

  @Test
  @DisplayName("WorkspaceRole 비교 규칙은 level 기반으로 고정된다")
  void workspaceRoleComparison_isLevelBased() {
    assertThat(WorkspaceRole.ADMIN.getLevel()).isEqualTo(2);
    assertThat(WorkspaceRole.MEMBER.getLevel()).isEqualTo(1);
    assertThat(WorkspaceRole.ADMIN.isHigherOrEqualThan(WorkspaceRole.MEMBER)).isTrue();
    assertThat(WorkspaceRole.MEMBER.isHigherOrEqualThan(WorkspaceRole.ADMIN)).isFalse();
  }

  @Test
  @DisplayName("InvitationStatus name은 DB 저장 값으로 고정된다")
  void invitationStatusNames_areFixed() {
    assertThat(InvitationStatus.PENDING.name()).isEqualTo("PENDING");
    assertThat(InvitationStatus.ACCEPTED.name()).isEqualTo("ACCEPTED");
    assertThat(InvitationStatus.REJECTED.name()).isEqualTo("REJECTED");
    assertThat(InvitationStatus.CANCELLED.name()).isEqualTo("CANCELLED");
  }

  @Test
  @DisplayName("InvitationType name은 DB 저장 값으로 고정된다")
  void invitationTypeNames_areFixed() {
    assertThat(InvitationType.WORKSPACE.name()).isEqualTo("WORKSPACE");
    assertThat(InvitationType.PROJECT.name()).isEqualTo("PROJECT");
  }

}
