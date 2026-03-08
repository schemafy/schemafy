package com.schemafy.core.project.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.project.repository.vo.InvitationStatus;
import com.schemafy.core.project.repository.vo.InvitationType;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.WorkspaceRole;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enum DB 저장 계약 테스트")
class EnumDbContractTest {

  @Test
  @DisplayName("ProjectRole의 name은 DB에 저장되는 고정 값이다")
  void projectRole_names_areFixed() {
    assertThat(ProjectRole.ADMIN.name()).isEqualTo("ADMIN");
    assertThat(ProjectRole.EDITOR.name()).isEqualTo("EDITOR");
    assertThat(ProjectRole.VIEWER.name()).isEqualTo("VIEWER");
  }

  @Test
  @DisplayName("WorkspaceRole의 name은 DB에 저장되는 고정 값이다")
  void workspaceRole_names_areFixed() {
    assertThat(WorkspaceRole.ADMIN.name()).isEqualTo("ADMIN");
    assertThat(WorkspaceRole.MEMBER.name()).isEqualTo("MEMBER");
  }

  @Test
  @DisplayName("InvitationStatus의 name은 DB에 저장되는 고정 값이다")
  void invitationStatus_names_areFixed() {
    assertThat(InvitationStatus.PENDING.name()).isEqualTo("PENDING");
    assertThat(InvitationStatus.ACCEPTED.name()).isEqualTo("ACCEPTED");
    assertThat(InvitationStatus.REJECTED.name()).isEqualTo("REJECTED");
    assertThat(InvitationStatus.CANCELLED.name()).isEqualTo("CANCELLED");
  }

  @Test
  @DisplayName("InvitationType의 name은 DB에 저장되는 고정 값이다")
  void invitationType_names_areFixed() {
    assertThat(InvitationType.WORKSPACE.name()).isEqualTo("WORKSPACE");
    assertThat(InvitationType.PROJECT.name()).isEqualTo("PROJECT");
  }

}
