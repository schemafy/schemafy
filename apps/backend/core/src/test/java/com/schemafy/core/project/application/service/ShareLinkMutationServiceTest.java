package com.schemafy.core.project.application.service;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.DeleteShareLinkCommand;
import com.schemafy.core.project.application.port.in.RevokeShareLinkCommand;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.ShareLink;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("공유 링크 변경 서비스 테스트")
class ShareLinkMutationServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String SHARE_LINK_ID = "share-link-id";

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  ShareLinkPort shareLinkPort;

  @InjectMocks
  DeleteShareLinkService deleteSut;

  @InjectMocks
  RevokeShareLinkService revokeSut;

  @BeforeEach
  void setUp() {
    lenient().when(transactionalOperator.<Void>transactional(
        org.mockito.ArgumentMatchers.<Mono<Void>>any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(transactionalOperator.<ShareLink>transactional(
        org.mockito.ArgumentMatchers.<Mono<ShareLink>>any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("삭제 결과 행 수만으로 활성 공유 링크를 삭제한다")
  void deleteShareLinkUsesConditionalUpdateWithoutPreRead() {
    DeleteShareLinkCommand command = new DeleteShareLinkCommand(PROJECT_ID,
        SHARE_LINK_ID, "requester-id");
    given(shareLinkPort.softDeleteByIdAndProjectId(SHARE_LINK_ID, PROJECT_ID))
        .willReturn(Mono.just(1L));

    StepVerifier.create(deleteSut.deleteShareLink(command))
        .verifyComplete();

    then(shareLinkPort).should().softDeleteByIdAndProjectId(SHARE_LINK_ID,
        PROJECT_ID);
    then(shareLinkPort).should(never()).findByIdAndProjectIdAndNotDeleted(
        SHARE_LINK_ID, PROJECT_ID);
  }

  @Test
  @DisplayName("폐기 결과 행 수로 판정한 뒤 변경된 공유 링크를 조회한다")
  void revokeShareLinkUsesConditionalUpdateWithoutPreRead() {
    RevokeShareLinkCommand command = new RevokeShareLinkCommand(PROJECT_ID,
        SHARE_LINK_ID, "requester-id");
    ShareLink revokedLink = ShareLink.create(SHARE_LINK_ID, PROJECT_ID, "code");
    revokedLink.revoke();
    given(shareLinkPort.revokeByIdAndProjectId(SHARE_LINK_ID, PROJECT_ID))
        .willReturn(Mono.just(1L));
    given(shareLinkPort.findByIdAndProjectIdAndNotDeleted(SHARE_LINK_ID,
        PROJECT_ID)).willReturn(Mono.just(revokedLink));

    StepVerifier.create(revokeSut.revokeShareLink(command))
        .expectNext(revokedLink)
        .verifyComplete();

    then(shareLinkPort).should().revokeByIdAndProjectId(SHARE_LINK_ID,
        PROJECT_ID);
  }

}
