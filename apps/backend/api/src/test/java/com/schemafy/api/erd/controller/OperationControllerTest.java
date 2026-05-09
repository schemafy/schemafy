package com.schemafy.api.erd.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.collaboration.constant.CollaborationConstants;
import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.security.WithMockCustomUser;
import com.schemafy.api.erd.docs.OperationApiSnippets;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.service.UndoRedoErdOperationService;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;

import reactor.core.publisher.Mono;

import static com.schemafy.api.erd.controller.ErdOperationFixtures.OP_ID;
import static com.schemafy.api.erd.controller.ErdOperationFixtures.committedOperation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("OperationController 통합 테스트")
@WithMockCustomUser(roles = "EDITOR")
class OperationControllerTest {

  private static final String API_BASE_PATH = ApiPath.API
      .replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private UndoRedoErdOperationService undoRedoErdOperationService;

  @Test
  @DisplayName("undo API 문서화")
  void undo() {
    given(undoRedoErdOperationService.undo(any(UndoErdOperationCommand.class)))
        .willReturn(Mono.just(
            MutationResult.<Void>of(null, Set.of("table-1"))
                .withOperation(
                    committedOperation(ErdOperationDerivationKind.UNDO))));

    webTestClient.post()
        .uri(API_BASE_PATH + "/operations/{opId}/undo", OP_ID)
        .header("Accept", "application/json")
        .header(CollaborationConstants.SESSION_ID_HEADER, "session-1")
        .header(CollaborationConstants.CLIENT_OPERATION_ID_HEADER, "client-op-undo")
        .header(CollaborationConstants.BASE_SCHEMA_REVISION_HEADER, "41")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.data").isEqualTo(null)
        .jsonPath("$.affectedTableIds").isArray()
        .jsonPath("$.operation.opId").isEqualTo(OP_ID)
        .jsonPath("$.operation.derivationKind").isEqualTo("UNDO")
        .consumeWith(document("operation-undo",
            OperationApiSnippets.undoPathParameters(),
            OperationApiSnippets.undoRequestHeaders(),
            OperationApiSnippets.undoResponseHeaders(),
            OperationApiSnippets.undoResponse()));

    then(undoRedoErdOperationService).should()
        .undo(new UndoErdOperationCommand(OP_ID));
  }

  @Test
  @DisplayName("redo API 문서화")
  void redo() {
    given(undoRedoErdOperationService.redo(any(RedoErdOperationCommand.class)))
        .willReturn(Mono.just(
            MutationResult.<Void>of(null, Set.of("table-1"))
                .withOperation(
                    committedOperation(ErdOperationDerivationKind.REDO))));

    webTestClient.post()
        .uri(API_BASE_PATH + "/operations/{opId}/redo", OP_ID)
        .header("Accept", "application/json")
        .header(CollaborationConstants.SESSION_ID_HEADER, "session-1")
        .header(CollaborationConstants.CLIENT_OPERATION_ID_HEADER, "client-op-redo")
        .header(CollaborationConstants.BASE_SCHEMA_REVISION_HEADER, "41")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.data").isEqualTo(null)
        .jsonPath("$.affectedTableIds").isArray()
        .jsonPath("$.operation.opId").isEqualTo(OP_ID)
        .jsonPath("$.operation.derivationKind").isEqualTo("REDO")
        .consumeWith(document("operation-redo",
            OperationApiSnippets.redoPathParameters(),
            OperationApiSnippets.redoRequestHeaders(),
            OperationApiSnippets.redoResponseHeaders(),
            OperationApiSnippets.redoResponse()));

    then(undoRedoErdOperationService).should()
        .redo(new RedoErdOperationCommand(OP_ID));
  }

  @Test
  @DisplayName("undo API는 존재하지 않는 opId면 404를 반환한다")
  void undoReturnsNotFound() {
    given(undoRedoErdOperationService.undo(any(UndoErdOperationCommand.class)))
        .willReturn(Mono.error(new DomainException(
            OperationErrorCode.NOT_FOUND,
            "Operation not found: " + OP_ID)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/operations/{opId}/undo", OP_ID)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.reason").isEqualTo(OperationErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("undo API는 superseded 상태면 409를 반환한다")
  void undoReturnsSuperseded() {
    given(undoRedoErdOperationService.undo(any(UndoErdOperationCommand.class)))
        .willReturn(Mono.error(new DomainException(
            OperationErrorCode.SUPERSEDED)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/operations/{opId}/undo", OP_ID)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.reason").isEqualTo(OperationErrorCode.SUPERSEDED.code());
  }

  @Test
  @DisplayName("undo API는 이미 undone 된 대상이면 409를 반환한다")
  void undoReturnsAlreadyUndone() {
    given(undoRedoErdOperationService.undo(any(UndoErdOperationCommand.class)))
        .willReturn(Mono.error(new DomainException(
            OperationErrorCode.ALREADY_UNDONE)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/operations/{opId}/undo", OP_ID)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.reason")
        .isEqualTo(OperationErrorCode.ALREADY_UNDONE.code());
  }

  @Test
  @DisplayName("undo API는 지원되지 않는 연산군이면 409를 반환한다")
  void undoReturnsUnsupported() {
    given(undoRedoErdOperationService.undo(any(UndoErdOperationCommand.class)))
        .willReturn(Mono.error(new DomainException(
            OperationErrorCode.UNSUPPORTED)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/operations/{opId}/undo", OP_ID)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.reason").isEqualTo(OperationErrorCode.UNSUPPORTED.code());
  }

  @Test
  @DisplayName("redo API는 존재하지 않는 opId면 404를 반환한다")
  void redoReturnsNotFound() {
    given(undoRedoErdOperationService.redo(any(RedoErdOperationCommand.class)))
        .willReturn(Mono.error(new DomainException(
            OperationErrorCode.NOT_FOUND,
            "Operation not found: " + OP_ID)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/operations/{opId}/redo", OP_ID)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.reason").isEqualTo(OperationErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("redo API는 redo 불가 상태면 409를 반환한다")
  void redoReturnsRedoNotEligible() {
    given(undoRedoErdOperationService.redo(any(RedoErdOperationCommand.class)))
        .willReturn(Mono.error(new DomainException(
            OperationErrorCode.REDO_NOT_ELIGIBLE)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/operations/{opId}/redo", OP_ID)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.reason")
        .isEqualTo(OperationErrorCode.REDO_NOT_ELIGIBLE.code());
  }

  @Test
  @DisplayName("redo API는 지원되지 않는 연산군이면 409를 반환한다")
  void redoReturnsUnsupported() {
    given(undoRedoErdOperationService.redo(any(RedoErdOperationCommand.class)))
        .willReturn(Mono.error(new DomainException(
            OperationErrorCode.UNSUPPORTED)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/operations/{opId}/redo", OP_ID)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.reason").isEqualTo(OperationErrorCode.UNSUPPORTED.code());
  }

}
