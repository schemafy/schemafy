package com.schemafy.core.erd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.WithMockCustomUser;
import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.core.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.core.erd.controller.dto.response.MemoDetailResponse;
import com.schemafy.core.erd.controller.dto.response.MemoResponse;
import com.schemafy.core.erd.service.MemoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
    private MemoService memoService;

    @Test
    @DisplayName("메모 생성 API 문서화")
    void createMemo() throws Exception {
        CreateMemoRequest request = new CreateMemoRequest(
                "06D6VZBWHSDJBBG0H7D156YZ98",
                "{}",
                "메모 내용");

        MemoDetailResponse response = objectMapper.treeToValue(
                objectMapper.readTree("""
                        {
                            "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "author": {
                                "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                "email": "test@example.com",
                                "name": "testuser"
                            },
                            "positions": "{}",
                            "createdAt": "2025-11-23T10:00:00Z",
                            "updatedAt": "2025-11-23T10:00:00Z",
                            "comments": [
                                {
                                    "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                    "memoId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                                    "author": {
                                        "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                        "email": "test@example.com",
                                        "name": "testuser"
                                    },
                                    "body": "메모 내용",
                                    "createdAt": "2025-11-23T10:00:00Z",
                                    "updatedAt": "2025-11-23T10:00:00Z"
                                }
                            ]
                        }
                        """), MemoDetailResponse.class);

        given(memoService.createMemo(any(CreateMemoRequest.class), any()))
                .willReturn(Mono.just(response));

        webTestClient.post()
                .uri(API_BASE_PATH + "/memos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
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
                                        .description("메모 위치 (JSON 문자열)"),
                                fieldWithPath("body").description("메모 내용")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("생성된 메모 정보"),
                                fieldWithPath("result.id")
                                        .description("메모 ID"),
                                fieldWithPath("result.schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("result.author")
                                        .description("작성자 정보"),
                                fieldWithPath("result.author.id")
                                        .description("작성자 ID"),
                                fieldWithPath("result.author.email")
                                        .description("작성자 이메일"),
                                fieldWithPath("result.author.name")
                                        .description("작성자 이름"),
                                fieldWithPath("result.positions")
                                        .description("메모 위치"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.comments")
                                        .description("메모 댓글 목록"),
                                fieldWithPath("result.comments[].id")
                                        .description("댓글 ID"),
                                fieldWithPath("result.comments[].memoId")
                                        .description("메모 ID"),
                                fieldWithPath("result.comments[].author")
                                        .description("작성자 정보"),
                                fieldWithPath("result.comments[].author.id")
                                        .description("작성자 ID"),
                                fieldWithPath(
                                        "result.comments[].author.email")
                                        .description("작성자 이메일"),
                                fieldWithPath(
                                        "result.comments[].author.name")
                                        .description("작성자 이름"),
                                fieldWithPath("result.comments[].body")
                                        .description("댓글 내용"),
                                fieldWithPath("result.comments[].createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.comments[].updatedAt")
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
                                "email": "test@example.com",
                                "name": "testuser"
                            },
                            "positions": "{}",
                            "createdAt": "2025-11-23T10:00:00Z",
                            "updatedAt": "2025-11-23T10:00:00Z",
                            "comments": [
                                {
                                    "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                    "memoId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                                    "author": {
                                        "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                        "email": "test@example.com",
                                        "name": "testuser"
                                    },
                                    "body": "메모 내용",
                                    "createdAt": "2025-11-23T10:00:00Z",
                                    "updatedAt": "2025-11-23T10:00:00Z"
                                }
                            ]
                        }
                        """), MemoDetailResponse.class);

        given(memoService.getMemo(memoId))
                .willReturn(Mono.just(response));

        webTestClient.get()
                .uri(API_BASE_PATH + "/memos/{memoId}", memoId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
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
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("메모 정보"),
                                fieldWithPath("result.id")
                                        .description("메모 ID"),
                                fieldWithPath("result.schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("result.author")
                                        .description("작성자 정보"),
                                fieldWithPath("result.author.id")
                                        .description("작성자 ID"),
                                fieldWithPath("result.author.email")
                                        .description("작성자 이메일"),
                                fieldWithPath("result.author.name")
                                        .description("작성자 이름"),
                                fieldWithPath("result.positions")
                                        .description("메모 위치"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.comments")
                                        .description("메모 댓글 목록"),
                                fieldWithPath("result.comments[].id")
                                        .description("댓글 ID"),
                                fieldWithPath("result.comments[].memoId")
                                        .description("메모 ID"),
                                fieldWithPath("result.comments[].author")
                                        .description("작성자 정보"),
                                fieldWithPath("result.comments[].author.id")
                                        .description("작성자 ID"),
                                fieldWithPath(
                                        "result.comments[].author.email")
                                        .description("작성자 이메일"),
                                fieldWithPath(
                                        "result.comments[].author.name")
                                        .description("작성자 이름"),
                                fieldWithPath("result.comments[].body")
                                        .description("댓글 내용"),
                                fieldWithPath("result.comments[].createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.comments[].updatedAt")
                                        .description("수정 일시"))));
    }

    @Test
    @DisplayName("메모 수정 API 문서화")
    void updateMemo() throws Exception {
        String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";
        UpdateMemoRequest request = new UpdateMemoRequest("{}");

        MemoResponse response = objectMapper.treeToValue(
                objectMapper.readTree("""
                        {
                            "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "author": {
                                "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                "email": "test@example.com",
                                "name": "testuser"
                            },
                            "positions": "{}",
                            "createdAt": "2025-11-23T10:00:00Z",
                            "updatedAt": "2025-11-23T10:00:00Z"
                        }
                        """), MemoResponse.class);

        given(memoService.updateMemo(eq(memoId), any(UpdateMemoRequest.class),
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
                .jsonPath("$.success").isEqualTo(true)
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
                                        .description("변경할 위치 정보")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("수정된 메모 정보"),
                                fieldWithPath("result.id")
                                        .description("메모 ID"),
                                fieldWithPath("result.schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("result.author")
                                        .description("작성자 정보"),
                                fieldWithPath("result.author.id")
                                        .description("작성자 ID"),
                                fieldWithPath("result.author.email")
                                        .description("작성자 이메일"),
                                fieldWithPath("result.author.name")
                                        .description("작성자 이름"),
                                fieldWithPath("result.positions")
                                        .description("메모 위치"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"))));
    }

    @Test
    @DisplayName("메모 삭제 API 문서화")
    void deleteMemo() throws Exception {
        String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";

        given(memoService.deleteMemo(eq(memoId), any(AuthenticatedUser.class)))
                .willReturn(Mono.empty());

        webTestClient.delete()
                .uri(API_BASE_PATH + "/memos/{memoId}", memoId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("memo-delete",
                        pathParameters(
                                parameterWithName("memoId")
                                        .description("삭제할 메모 ID")),
                        requestHeaders(
                                headerWithName("Accept")
                                        .description(
                                                "응답 포맷 (application/json)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .type(JsonFieldType.NULL)
                                        .description("응답 데이터 (null)")
                                        .optional())));
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
                                "email": "test@example.com",
                                "name": "testuser"
                            },
                            "body": "댓글 내용",
                            "createdAt": "2025-11-23T10:00:00Z",
                            "updatedAt": "2025-11-23T10:00:00Z"
                        }
                        """), MemoCommentResponse.class);

        given(memoService.createComment(eq(memoId),
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
                .jsonPath("$.success").isEqualTo(true)
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
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("생성된 댓글 정보"),
                                fieldWithPath("result.id")
                                        .description("댓글 ID"),
                                fieldWithPath("result.memoId")
                                        .description("메모 ID"),
                                fieldWithPath("result.author")
                                        .description("작성자 정보"),
                                fieldWithPath("result.author.id")
                                        .description("작성자 ID"),
                                fieldWithPath("result.author.email")
                                        .description("작성자 이메일"),
                                fieldWithPath("result.author.name")
                                        .description("작성자 이름"),
                                fieldWithPath("result.body")
                                        .description("댓글 내용"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
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
                                "email": "test@example.com",
                                "name": "testuser"
                            },
                            "body": "댓글 내용",
                            "createdAt": "2025-11-23T10:00:00Z",
                            "updatedAt": "2025-11-23T10:00:00Z"
                        }
                        """), MemoCommentResponse.class);

        given(memoService.getComments(memoId))
                .willReturn(Flux.just(response));

        webTestClient.get()
                .uri(API_BASE_PATH + "/memos/{memoId}/comments", memoId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
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
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("댓글 목록"),
                                fieldWithPath("result[].id")
                                        .description("댓글 ID"),
                                fieldWithPath("result[].memoId")
                                        .description("메모 ID"),
                                fieldWithPath("result[].author")
                                        .description("작성자 정보"),
                                fieldWithPath("result[].author.id")
                                        .description("작성자 ID"),
                                fieldWithPath("result[].author.email")
                                        .description("작성자 이메일"),
                                fieldWithPath("result[].author.name")
                                        .description("작성자 이름"),
                                fieldWithPath("result[].body")
                                        .description("댓글 내용"),
                                fieldWithPath("result[].createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result[].updatedAt")
                                        .description("수정 일시"))));
    }

    @Test
    @DisplayName("메모 댓글 수정 API 문서화")
    void updateMemoComment() throws Exception {
        String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";
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
                                "email": "test@example.com",
                                "name": "testuser"
                            },
                            "body": "수정된 내용",
                            "createdAt": "2025-11-23T10:00:00Z",
                            "updatedAt": "2025-11-23T10:00:00Z"
                        }
                        """), MemoCommentResponse.class);

        given(memoService.updateComment(eq(memoId), eq(commentId),
                any(UpdateMemoCommentRequest.class), any()))
                .willReturn(Mono.just(response));

        webTestClient.put()
                .uri(API_BASE_PATH + "/memos/{memoId}/comments/{commentId}",
                        memoId, commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("memo-comment-update",
                        pathParameters(
                                parameterWithName("memoId")
                                        .description("메모 ID"),
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
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("수정된 댓글 정보"),
                                fieldWithPath("result.id")
                                        .description("댓글 ID"),
                                fieldWithPath("result.memoId")
                                        .description("메모 ID"),
                                fieldWithPath("result.author")
                                        .description("작성자 정보"),
                                fieldWithPath("result.author.id")
                                        .description("작성자 ID"),
                                fieldWithPath("result.author.email")
                                        .description("작성자 이메일"),
                                fieldWithPath("result.author.name")
                                        .description("작성자 이름"),
                                fieldWithPath("result.body")
                                        .description("댓글 내용"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"))));
    }

    @Test
    @DisplayName("메모 댓글 삭제 API 문서화")
    void deleteMemoComment() throws Exception {
        String memoId = "06D6W1GAHD51T5NJPK29Q6BCR8";
        String commentId = "06D6WCH677C3FCC2Q9SD5M1Y5W";

        given(memoService.deleteComment(eq(memoId), eq(commentId),
                any(AuthenticatedUser.class)))
                .willReturn(Mono.empty());

        webTestClient.delete()
                .uri(API_BASE_PATH + "/memos/{memoId}/comments/{commentId}",
                        memoId, commentId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("memo-comment-delete",
                        pathParameters(
                                parameterWithName("memoId")
                                        .description("메모 ID"),
                                parameterWithName("commentId")
                                        .description("삭제할 댓글 ID")),
                        requestHeaders(
                                headerWithName("Accept")
                                        .description(
                                                "응답 포맷 (application/json)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .type(JsonFieldType.NULL)
                                        .description("응답 데이터 (null)")
                                        .optional())));
    }

}
