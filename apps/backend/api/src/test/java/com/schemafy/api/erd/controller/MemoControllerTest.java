package com.schemafy.api.erd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.common.security.WithMockCustomUser;
import com.schemafy.api.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.api.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.api.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.api.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.api.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.api.erd.controller.dto.response.MemoDetailResponse;
import com.schemafy.api.erd.controller.dto.response.MemoResponse;
import com.schemafy.api.erd.service.MemoOrchestrator;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommand;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommentUseCase;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoUseCase;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("MemoController 통합 테스트")
@WithMockCustomUser(roles = "EDITOR")
class MemoControllerTest {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  private static final String API_BASE_PATH = ApiPath.API
      .replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private MemoOrchestrator memoOrchestrator;

  @MockitoBean
  private DeleteMemoUseCase deleteMemoUseCase;

  @MockitoBean
  private DeleteMemoCommentUseCase deleteMemoCommentUseCase;

  @Test
  @DisplayName("메모 생성 API 문서화")
  void createMemo() throws Exception {
    CreateMemoRequest request = new CreateMemoRequest(
        "06D6VZBWHSDJBBG0H7D156YZ98",
        objectMapper.readTree("{\"x\":0,\"y\":0}"),
        "메모 내용");

    MemoDetailResponse response = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
                "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                "author": {
                    "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    "name": "testuser"
                },
                "positions": {"x":0,"y":0},
                "createdAt": "2025-11-23T10:00:00Z",
                "updatedAt": "2025-11-23T10:00:00Z",
                "comments": [
                    {
                        "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                        "memoId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                        "author": {
                            "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                            "name": "testuser"
                        },
                        "body": "메모 내용",
                        "createdAt": "2025-11-23T10:00:00Z",
                        "updatedAt": "2025-11-23T10:00:00Z"
                    }
                ]
            }
            """), MemoDetailResponse.class);

    given(memoOrchestrator.createMemo(any(CreateMemoRequest.class), any()))
        .willReturn(Mono.just(response));

    webTestClient.post()
        .uri(API_BASE_PATH + "/memos")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("memo-create",
            requestHeaders(
                headerWithName("Content-Type")
                    .description(
                        "요청 본문 타입 (application/json)"),
                headerWithName("Accept")
                    .description(
                        "응답 포맷 (application/json)")),
            requestFields(
                fieldWithPath("schemaId").description("스키마 ID"),
                fieldWithPath("positions")
                    .description("메모 위치 객체"),
                fieldWithPath("positions.x")
                    .description("메모 X 좌표"),
                fieldWithPath("positions.y")
                    .description("메모 Y 좌표"),
                fieldWithPath("body").description("메모 내용")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("id")
                    .description("메모 ID"),
                fieldWithPath("schemaId")
                    .description("스키마 ID"),
                fieldWithPath("author")
                    .description("작성자 정보"),
                fieldWithPath("author.id")
                    .description("작성자 ID"),
                fieldWithPath("author.name")
                    .description("작성자 이름"),
                fieldWithPath("positions")
                    .description("메모 위치 객체"),
                fieldWithPath("positions.x")
                    .description("메모 X 좌표"),
                fieldWithPath("positions.y")
                    .description("메모 Y 좌표"),
                fieldWithPath("createdAt")
                    .description("생성 일시"),
                fieldWithPath("updatedAt")
                    .description("수정 일시"),
                fieldWithPath("comments")
                    .description("메모 댓글 목록"),
                fieldWithPath("comments[].id")
                    .description("댓글 ID"),
                fieldWithPath("comments[].memoId")
                    .description("메모 ID"),
                fieldWithPath("comments[].author")
                    .description("작성자 정보"),
                fieldWithPath("comments[].author.id")
                    .description("작성자 ID"),
                fieldWithPath(
                    "comments[].author.name")
                    .description("작성자 이름"),
                fieldWithPath("comments[].body")
                    .description("댓글 내용"),
                fieldWithPath("comments[].createdAt")
                    .description("생성 일시"),
                fieldWithPath("comments[].updatedAt")
                    .description("수정 일시"))));
  }

  @Test
  @DisplayName("메모 조회 API 문서화")
  void getMemo() throws Exception {
    String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";

    MemoDetailResponse response = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
                "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                "author": {
                    "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    "name": "testuser"
                },
                "positions": {"x":0,"y":0},
                "createdAt": "2025-11-23T10:00:00Z",
                "updatedAt": "2025-11-23T10:00:00Z",
                "comments": [
                    {
                        "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                        "memoId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                        "author": {
                            "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                            "name": "testuser"
                        },
                        "body": "메모 내용",
                        "createdAt": "2025-11-23T10:00:00Z",
                        "updatedAt": "2025-11-23T10:00:00Z"
                    }
                ]
            }
            """), MemoDetailResponse.class);

    given(memoOrchestrator.getMemo(memoId))
        .willReturn(Mono.just(response));

    webTestClient.get()
        .uri(API_BASE_PATH + "/memos/{memoId}", memoId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("memo-get",
            pathParameters(
                parameterWithName("memoId")
                    .description("조회할 메모 ID")),
            requestHeaders(
                headerWithName("Accept")
                    .description(
                        "응답 포맷 (application/json)")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("id")
                    .description("메모 ID"),
                fieldWithPath("schemaId")
                    .description("스키마 ID"),
                fieldWithPath("author")
                    .description("작성자 정보"),
                fieldWithPath("author.id")
                    .description("작성자 ID"),
                fieldWithPath("author.name")
                    .description("작성자 이름"),
                fieldWithPath("positions")
                    .description("메모 위치 객체"),
                fieldWithPath("positions.x")
                    .description("메모 X 좌표"),
                fieldWithPath("positions.y")
                    .description("메모 Y 좌표"),
                fieldWithPath("createdAt")
                    .description("생성 일시"),
                fieldWithPath("updatedAt")
                    .description("수정 일시"),
                fieldWithPath("comments")
                    .description("메모 댓글 목록"),
                fieldWithPath("comments[].id")
                    .description("댓글 ID"),
                fieldWithPath("comments[].memoId")
                    .description("메모 ID"),
                fieldWithPath("comments[].author")
                    .description("작성자 정보"),
                fieldWithPath("comments[].author.id")
                    .description("작성자 ID"),
                fieldWithPath(
                    "comments[].author.name")
                    .description("작성자 이름"),
                fieldWithPath("comments[].body")
                    .description("댓글 내용"),
                fieldWithPath("comments[].createdAt")
                    .description("생성 일시"),
                fieldWithPath("comments[].updatedAt")
                    .description("수정 일시"))));
  }

  @Test
  @DisplayName("스키마별 메모 목록 조회")
  void getMemosBySchemaId() throws Exception {
    String schemaId = "06D6VZBWHSDJBBG0H7D156YZ98";

    MemoResponse response = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
                "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                "author": {
                    "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    "name": "testuser"
                },
                "positions": {"x":0,"y":0},
                "createdAt": "2025-11-23T10:00:00Z",
                "updatedAt": "2025-11-23T10:00:00Z"
            }
            """), MemoResponse.class);

    given(memoOrchestrator.getMemosBySchemaId(schemaId))
        .willReturn(Flux.just(response));

    webTestClient.get()
        .uri(API_BASE_PATH + "/schemas/{schemaId}/memos", schemaId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$").isArray()
        .jsonPath("$[0].schemaId").isEqualTo(schemaId);
  }

  @Test
  @DisplayName("메모 수정 API 문서화")
  void updateMemo() throws Exception {
    String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";
    UpdateMemoRequest request = new UpdateMemoRequest(
        objectMapper.readTree("{\"x\":0,\"y\":0}"));

    MemoResponse response = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
                "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                "author": {
                    "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    "name": "testuser"
                },
                "positions": {"x":0,"y":0},
                "createdAt": "2025-11-23T10:00:00Z",
                "updatedAt": "2025-11-23T10:00:00Z"
            }
            """), MemoResponse.class);

    given(memoOrchestrator.updateMemo(eq(memoId), any(UpdateMemoRequest.class),
        any()))
        .willReturn(Mono.just(response));

    webTestClient.put()
        .uri(API_BASE_PATH + "/memos/{memoId}", memoId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("memo-update",
            pathParameters(
                parameterWithName("memoId")
                    .description("수정할 메모 ID")),
            requestHeaders(
                headerWithName("Content-Type")
                    .description(
                        "요청 본문 타입 (application/json)"),
                headerWithName("Accept")
                    .description(
                        "응답 포맷 (application/json)")),
            requestFields(
                fieldWithPath("positions")
                    .description("변경할 위치 정보 객체"),
                fieldWithPath("positions.x")
                    .description("메모 X 좌표"),
                fieldWithPath("positions.y")
                    .description("메모 Y 좌표")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("id")
                    .description("메모 ID"),
                fieldWithPath("schemaId")
                    .description("스키마 ID"),
                fieldWithPath("author")
                    .description("작성자 정보"),
                fieldWithPath("author.id")
                    .description("작성자 ID"),
                fieldWithPath("author.name")
                    .description("작성자 이름"),
                fieldWithPath("positions")
                    .description("메모 위치 객체"),
                fieldWithPath("positions.x")
                    .description("메모 X 좌표"),
                fieldWithPath("positions.y")
                    .description("메모 Y 좌표"),
                fieldWithPath("createdAt")
                    .description("생성 일시"),
                fieldWithPath("updatedAt")
                    .description("수정 일시"))));
  }

  @Test
  @DisplayName("메모 생성 API는 positions 객체를 허용한다")
  void createMemoAcceptsObjectValue() throws Exception {
    given(memoOrchestrator.createMemo(any(CreateMemoRequest.class), any()))
        .willReturn(Mono.just(MemoDetailResponse.builder()
            .id("06D6W1GAHD51T5NJPK29Q6BCR8")
            .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
            .positions(objectMapper.readTree("{\"x\":10,\"y\":20}"))
            .comments(java.util.List.of())
            .build()));

    webTestClient.post()
        .uri(API_BASE_PATH + "/memos")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
              "positions": {"x":10,"y":20},
              "body": "메모 내용"
            }
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk();

    then(memoOrchestrator).should().createMemo(
        eq(new CreateMemoRequest(
            "06D6VZBWHSDJBBG0H7D156YZ98",
            objectMapper.readTree("{\"x\":10,\"y\":20}"),
            "메모 내용")),
        any());
  }

  @Test
  @DisplayName("메모 생성 API는 문자열 positions를 거절한다")
  void createMemoRejectsInvalidPositionsJsonString() {
    webTestClient.post()
        .uri(API_BASE_PATH + "/memos")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
              "positions": "{invalid",
              "body": "메모 내용"
            }
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.reason").isEqualTo(CommonErrorCode.INVALID_PARAMETER.code());

    then(memoOrchestrator).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("메모 수정 API는 positions 객체를 허용한다")
  void updateMemoAcceptsObjectValue() throws Exception {
    String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";

    given(memoOrchestrator.updateMemo(eq(memoId), any(UpdateMemoRequest.class), any()))
        .willReturn(Mono.just(new MemoResponse(
            memoId,
            "06D6VZBWHSDJBBG0H7D156YZ98",
            null,
            objectMapper.readTree("{\"x\":30,\"y\":40}"),
            null,
            null)));

    webTestClient.put()
        .uri(API_BASE_PATH + "/memos/{memoId}", memoId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"positions":{"x":30,"y":40}}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk();

    then(memoOrchestrator).should().updateMemo(
        eq(memoId),
        eq(new UpdateMemoRequest(objectMapper.readTree("{\"x\":30,\"y\":40}"))),
        any());
  }

  @Test
  @DisplayName("메모 수정 API는 문자열 positions를 거절한다")
  void updateMemoRejectsInvalidPositionsJsonString() {
    String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";

    webTestClient.put()
        .uri(API_BASE_PATH + "/memos/{memoId}", memoId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"positions":"{invalid"}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.reason").isEqualTo(CommonErrorCode.INVALID_PARAMETER.code());

    then(memoOrchestrator).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("메모 삭제 API 문서화")
  void deleteMemo() throws Exception {
    String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";

    given(deleteMemoUseCase.deleteMemo(any(DeleteMemoCommand.class)))
        .willReturn(Mono.empty());

    webTestClient.delete()
        .uri(API_BASE_PATH + "/memos/{memoId}", memoId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("memo-delete",
            pathParameters(
                parameterWithName("memoId")
                    .description("삭제할 메모 ID")),
            requestHeaders(
                headerWithName("Accept")
                    .description(
                        "응답 포맷 (application/json)"))));
  }

  @Test
  @DisplayName("메모 댓글 생성 API 문서화")
  void createMemoComment() throws Exception {
    String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";
    CreateMemoCommentRequest request = new CreateMemoCommentRequest(
        "댓글 내용");

    MemoCommentResponse response = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
                "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                "memoId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "author": {
                    "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    "name": "testuser"
                },
                "body": "댓글 내용",
                "createdAt": "2025-11-23T10:00:00Z",
                "updatedAt": "2025-11-23T10:00:00Z"
            }
            """), MemoCommentResponse.class);

    given(memoOrchestrator.createComment(eq(memoId),
        any(CreateMemoCommentRequest.class), any()))
        .willReturn(Mono.just(response));

    webTestClient.post()
        .uri(API_BASE_PATH + "/memos/{memoId}/comments", memoId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("memo-comment-create",
            pathParameters(
                parameterWithName("memoId")
                    .description("메모 ID")),
            requestHeaders(
                headerWithName("Content-Type")
                    .description(
                        "요청 본문 타입 (application/json)"),
                headerWithName("Accept")
                    .description(
                        "응답 포맷 (application/json)")),
            requestFields(
                fieldWithPath("body").description("댓글 내용")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("id")
                    .description("댓글 ID"),
                fieldWithPath("memoId")
                    .description("메모 ID"),
                fieldWithPath("author")
                    .description("작성자 정보"),
                fieldWithPath("author.id")
                    .description("작성자 ID"),
                fieldWithPath("author.name")
                    .description("작성자 이름"),
                fieldWithPath("body")
                    .description("댓글 내용"),
                fieldWithPath("createdAt")
                    .description("생성 일시"),
                fieldWithPath("updatedAt")
                    .description("수정 일시"))));
  }

  @Test
  @DisplayName("메모 댓글 목록 조회 API 문서화")
  void getMemoComments() throws Exception {
    String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";
    MemoCommentResponse response = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
                "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                "memoId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "author": {
                    "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    "name": "testuser"
                },
                "body": "댓글 내용",
                "createdAt": "2025-11-23T10:00:00Z",
                "updatedAt": "2025-11-23T10:00:00Z"
            }
            """), MemoCommentResponse.class);

    given(memoOrchestrator.getComments(memoId))
        .willReturn(Flux.just(response));

    webTestClient.get()
        .uri(API_BASE_PATH + "/memos/{memoId}/comments", memoId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("memo-comment-list",
            pathParameters(
                parameterWithName("memoId")
                    .description("메모 ID")),
            requestHeaders(
                headerWithName("Accept")
                    .description(
                        "응답 포맷 (application/json)")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("[].id")
                    .description("댓글 ID"),
                fieldWithPath("[].memoId")
                    .description("메모 ID"),
                fieldWithPath("[].author")
                    .description("작성자 정보"),
                fieldWithPath("[].author.id")
                    .description("작성자 ID"),
                fieldWithPath("[].author.name")
                    .description("작성자 이름"),
                fieldWithPath("[].body")
                    .description("댓글 내용"),
                fieldWithPath("[].createdAt")
                    .description("생성 일시"),
                fieldWithPath("[].updatedAt")
                    .description("수정 일시"))));
  }

  @Test
  @DisplayName("메모 댓글 수정 API 문서화")
  void updateMemoComment() throws Exception {
    String commentId = "06D6WCH677C3FCC2Q9SD5M1Y5W";
    UpdateMemoCommentRequest request = new UpdateMemoCommentRequest(
        "수정된 내용");

    MemoCommentResponse response = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
                "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                "memoId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "author": {
                    "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    "name": "testuser"
                },
                "body": "수정된 내용",
                "createdAt": "2025-11-23T10:00:00Z",
                "updatedAt": "2025-11-23T10:00:00Z"
            }
            """), MemoCommentResponse.class);

    given(memoOrchestrator.updateComment(eq(commentId),
        any(UpdateMemoCommentRequest.class), any()))
        .willReturn(Mono.just(response));

    webTestClient.put()
        .uri(API_BASE_PATH + "/memo-comments/{commentId}",
            commentId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("memo-comment-update",
            pathParameters(
                parameterWithName("commentId")
                    .description("댓글 ID")),
            requestHeaders(
                headerWithName("Content-Type")
                    .description(
                        "요청 본문 타입 (application/json)"),
                headerWithName("Accept")
                    .description(
                        "응답 포맷 (application/json)")),
            requestFields(
                fieldWithPath("body").description("수정할 내용")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("id")
                    .description("댓글 ID"),
                fieldWithPath("memoId")
                    .description("메모 ID"),
                fieldWithPath("author")
                    .description("작성자 정보"),
                fieldWithPath("author.id")
                    .description("작성자 ID"),
                fieldWithPath("author.name")
                    .description("작성자 이름"),
                fieldWithPath("body")
                    .description("댓글 내용"),
                fieldWithPath("createdAt")
                    .description("생성 일시"),
                fieldWithPath("updatedAt")
                    .description("수정 일시"))));
  }

  @Test
  @DisplayName("메모 댓글 삭제 API 문서화")
  void deleteMemoComment() throws Exception {
    String commentId = "06D6WCH677C3FCC2Q9SD5M1Y5W";

    given(deleteMemoCommentUseCase.deleteMemoComment(
        any(DeleteMemoCommentCommand.class)))
        .willReturn(Mono.empty());

    webTestClient.delete()
        .uri(API_BASE_PATH + "/memo-comments/{commentId}",
            commentId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("memo-comment-delete",
            pathParameters(
                parameterWithName("commentId")
                    .description("삭제할 댓글 ID")),
            requestHeaders(
                headerWithName("Accept")
                    .description(
                        "응답 포맷 (application/json)"))));
  }

}
