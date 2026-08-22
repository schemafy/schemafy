package com.schemafy.core.project.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceCommand;
import com.schemafy.core.project.application.port.in.WorkspaceDetail;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크스페이스 수정 서비스 테스트")
class UpdateWorkspaceServiceTest {

  @Mock
  WorkspacePort workspacePort;

  @Mock
  WorkspaceAccessHelper workspaceAccessHelper;

  @InjectMocks
  UpdateWorkspaceService sut;

  @Test
  @DisplayName("활성 워크스페이스 행을 수정한 뒤 최신 상세 정보를 조회한다")
  void updateWorkspace_updatesThenLoadsDetail() {
    Workspace workspace = Workspace.create("workspace-id", "변경 이름", "변경 설명");
    WorkspaceDetail detail = new WorkspaceDetail(workspace, 2L, "ADMIN");
    UpdateWorkspaceCommand command = new UpdateWorkspaceCommand(
        workspace.getId(), "변경 이름", "변경 설명", "requester-id");
    given(workspacePort.updateIfActive(workspace.getId(), "변경 이름", "변경 설명"))
        .willReturn(Mono.just(1L));
    given(workspaceAccessHelper.findWorkspaceOrThrow(workspace.getId()))
        .willReturn(Mono.just(workspace));
    given(workspaceAccessHelper.buildWorkspaceDetail(workspace, "requester-id"))
        .willReturn(Mono.just(detail));

    StepVerifier.create(sut.updateWorkspace(command))
        .expectNext(detail)
        .verifyComplete();

    then(workspacePort).should()
        .updateIfActive(workspace.getId(), "변경 이름", "변경 설명");
    then(workspaceAccessHelper).should().findWorkspaceOrThrow(workspace.getId());
  }

  @Test
  @DisplayName("수정된 행이 없으면 워크스페이스 없음 오류를 반환하고 상세 정보를 조회하지 않는다")
  void updateWorkspace_rejectsMissingWorkspaceWithoutLoadingDetail() {
    UpdateWorkspaceCommand command = new UpdateWorkspaceCommand(
        "workspace-id", "변경 이름", "변경 설명", "requester-id");
    given(workspacePort.updateIfActive("workspace-id", "변경 이름", "변경 설명"))
        .willReturn(Mono.just(0L));

    StepVerifier.create(sut.updateWorkspace(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(WorkspaceErrorCode.NOT_FOUND);
        })
        .verify();

    then(workspaceAccessHelper).should(never()).findWorkspaceOrThrow("workspace-id");
    then(workspaceAccessHelper).should(never())
        .buildWorkspaceDetail(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

}
