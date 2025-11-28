package com.schemafy.core.erd.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.common.security.principal.ProjectRole;
import com.schemafy.core.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.core.erd.controller.dto.response.MemoDetailResponse;
import com.schemafy.core.erd.repository.MemoCommentRepository;
import com.schemafy.core.erd.repository.MemoRepository;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.entity.Memo;
import com.schemafy.core.erd.repository.entity.MemoComment;
import com.schemafy.core.erd.repository.entity.Schema;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("MemoService 테스트")
class MemoServiceTest {

    @Autowired
    MemoService memoService;

    @Autowired
    MemoRepository memoRepository;

    @Autowired
    MemoCommentRepository memoCommentRepository;

    @Autowired
    SchemaRepository schemaRepository;

    @BeforeEach
    void setUp() {
        memoCommentRepository.deleteAll().block();
        memoRepository.deleteAll().block();
        schemaRepository.deleteAll().block();
    }

    @Test
    @DisplayName("createMemo: 스키마가 존재하면 메모와 초기 댓글을 생성한다")
    void createMemo_success() {
        Schema schema = schemaRepository.save(Schema.builder()
                .projectId("06D4K6TTEWXW8VQR8EZXDPWP3C")
                .dbVendorId("MYSQL")
                .name("test-schema")
                .build()).block();

        CreateMemoRequest request = new CreateMemoRequest(
                schema.getId(),
                "{\"x\":100,\"y\":100}",
                "초기 메모 내용");
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        AuthenticatedUser user = AuthenticatedUser.of(authorId);

        Mono<MemoDetailResponse> result = memoService.createMemo(request,
                user);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getSchemaId())
                            .isEqualTo(schema.getId());
                    assertThat(response.getAuthorId()).isEqualTo(authorId);
                    assertThat(response.getPositions())
                            .isEqualTo(request.positions());
                    assertThat(response.getComments()).hasSize(1);
                    assertThat(response.getComments().get(0).getBody())
                            .isEqualTo(request.body());
                    assertThat(response.getComments().get(0).getAuthorId())
                            .isEqualTo(authorId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createMemo: 스키마가 없으면 에러를 반환한다")
    void createMemo_schemaNotFound() {
        CreateMemoRequest request = new CreateMemoRequest(
                "06D6VZBWHSDJBBG0H7D156YZ99",
                "{\"x\":100,\"y\":100}",
                "내용");
        AuthenticatedUser user = AuthenticatedUser
                .of("01ARZ3NDEKTSV4RRFFQ69G5FAV");

        StepVerifier
                .create(memoService.createMemo(request, user))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_SCHEMA_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("getMemo: 메모 상세 정보를 조회한다")
    void getMemo_success() {
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .positions("{}")
                .build()).block();

        memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .body("댓글 1")
                .build()).block();

        memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("06D6W8HDY79QFZX39RMX62KSX4")
                .body("댓글 2")
                .build()).block();

        StepVerifier.create(memoService.getMemo(memo.getId()))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(memo.getId());
                    assertThat(response.getComments()).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getMemosBySchemaId: 스키마별 메모 목록을 조회한다")
    void getMemosBySchemaId_success() {
        String schemaId = "06D6VZBWHSDJBBG0H7D156YZ98";
        memoRepository.save(Memo.builder()
                .schemaId(schemaId)
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .positions("{}")
                .build()).block();
        memoRepository.save(Memo.builder()
                .schemaId(schemaId)
                .authorId("06D6W8HDY79QFZX39RMX62KSX4")
                .positions("{}")
                .build()).block();
        memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ99")
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .positions("{}")
                .build()).block();

        StepVerifier
                .create(memoService.getMemosBySchemaId(schemaId).collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateMemo: 작성자 본인이면 메모 위치를 수정한다")
    void updateMemo_success() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{\"x\":0,\"y\":0}")
                .build()).block();

        UpdateMemoRequest request = new UpdateMemoRequest(memo.getId(),
                "{\"x\":100,\"y\":100}");
        AuthenticatedUser user = AuthenticatedUser.of(authorId);

        StepVerifier.create(memoService.updateMemo(request, user))
                .assertNext(response -> {
                    assertThat(response.getPositions())
                            .isEqualTo(request.positions());
                })
                .verifyComplete();

        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(updated -> {
                    assertThat(updated.getPositions())
                            .isEqualTo(request.positions());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateMemo: 작성자가 아니면 권한 에러를 반환한다")
    void updateMemo_accessDenied() {
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .positions("{\"x\":0,\"y\":0}")
                .build()).block();

        UpdateMemoRequest request = new UpdateMemoRequest(memo.getId(),
                "{\"x\":100,\"y\":100}");
        AuthenticatedUser otherUser = AuthenticatedUser
                .of("06D6W8HDY79QFZX39RMX62KSX4");

        StepVerifier
                .create(memoService.updateMemo(request, otherUser))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ACCESS_DENIED)
                .verify();
    }

    @Test
    @DisplayName("deleteMemo: 작성자 본인이면 메모를 삭제한다 (Soft Delete, 댓글은 유지)")
    void deleteMemo_success() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{}")
                .build()).block();

        MemoComment comment = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("06D6W8HDY79QFZX39RMX62KSX4")
                .body("댓글")
                .build()).block();

        AuthenticatedUser user = AuthenticatedUser.of(authorId);

        StepVerifier.create(memoService.deleteMemo(memo.getId(), user))
                .verifyComplete();

        // 메모 삭제 확인
        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(m -> assertThat(m.getDeletedAt()).isNotNull())
                .verifyComplete();

        // 댓글은 삭제되지 않음 (상위 계층 삭제 시 하위 계층 유지 정책)
        StepVerifier.create(memoCommentRepository.findById(comment.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteMemo: 관리자(ADMIN)는 다른 사람의 메모를 삭제할 수 있다")
    void deleteMemo_admin_success() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{}")
                .build()).block();

        String adminId = "06D6WCH677C3FCC2Q9SD5M1Y5X"; // Admin User ID
        AuthenticatedUser adminUser = AuthenticatedUser.withRoles(adminId,
                Set.of(ProjectRole.ADMIN));

        StepVerifier.create(memoService.deleteMemo(memo.getId(), adminUser))
                .verifyComplete();

        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(m -> assertThat(m.getDeletedAt()).isNotNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("createComment: 메모 댓글을 생성한다")
    void createComment_success() {
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .positions("{}")
                .build()).block();

        CreateMemoCommentRequest request = new CreateMemoCommentRequest("새 댓글");
        String authorId = "06D6W8HDY79QFZX39RMX62KSX4";
        AuthenticatedUser user = AuthenticatedUser.of(authorId);

        StepVerifier
                .create(memoService.createComment(memo.getId(), request, user))
                .assertNext(response -> {
                    assertThat(response.getMemoId()).isEqualTo(memo.getId());
                    assertThat(response.getBody()).isEqualTo("새 댓글");
                    assertThat(response.getAuthorId()).isEqualTo(authorId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateComment: 작성자 본인이면 댓글을 수정한다")
    void updateComment_success() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{}")
                .build()).block();
        MemoComment comment = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("이전 내용")
                .build()).block();

        UpdateMemoCommentRequest request = new UpdateMemoCommentRequest(
                memo.getId(), comment.getId(), "수정 내용");
        AuthenticatedUser user = AuthenticatedUser.of(authorId);

        StepVerifier.create(memoService.updateComment(request, user))
                .assertNext(response -> {
                    assertThat(response.getBody()).isEqualTo("수정 내용");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateComment: 댓글이 다른 memoId에 속하면 INVALID_PARAMETER를 반환한다")
    void updateComment_memoMismatch() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{}")
                .build()).block();
        MemoComment comment = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("이전 내용")
                .build()).block();

        UpdateMemoCommentRequest request = new UpdateMemoCommentRequest(
                "06D6W1GAHD51T5NJPK29Q6BCR9", comment.getId(), "수정 내용");
        AuthenticatedUser user = AuthenticatedUser.of(authorId);

        StepVerifier.create(memoService.updateComment(request, user))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.COMMON_INVALID_PARAMETER)
                .verify();
    }

    @Test
    @DisplayName("deleteComment: 유일한 댓글(첫 댓글) 삭제 시 메모만 삭제된다")
    void deleteComment_firstAndOnlyComment() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .positions("{}")
                .build()).block();

        MemoComment comment = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("유일한 댓글")
                .build()).block();

        AuthenticatedUser user = AuthenticatedUser.of(authorId);

        StepVerifier
                .create(memoService.deleteComment(memo.getId(), comment.getId(),
                        user))
                .verifyComplete();

        // 메모 삭제 확인
        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(m -> assertThat(m.getDeletedAt()).isNotNull())
                .verifyComplete();

        // 댓글은 삭제되지 않음 (상위 계층 삭제 시 하위 계층 유지 정책)
        StepVerifier.create(memoCommentRepository.findById(comment.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteComment: 관리자(OWNER)는 다른 사람의 댓글을 삭제할 수 있다")
    void deleteComment_admin_success() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String otherAuthorId = "06D6W8HDY79QFZX39RMX62KSX4";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{}")
                .build()).block();

        // 첫 번째 댓글 (본문)
        memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("본문")
                .build()).block();

        // 두 번째 댓글 (다른 사용자의 댓글 - 삭제 대상)
        MemoComment targetComment = memoCommentRepository
                .save(MemoComment.builder()
                        .memoId(memo.getId())
                        .authorId(otherAuthorId)
                        .body("답글")
                        .build())
                .block();

        String ownerId = "06D6WCH677C3FCC2Q9SD5M1Y5Y"; // Owner User ID
        AuthenticatedUser ownerUser = AuthenticatedUser.withRoles(ownerId,
                Set.of(ProjectRole.OWNER));

        StepVerifier
                .create(memoService.deleteComment(memo.getId(),
                        targetComment.getId(), ownerUser))
                .verifyComplete();

        StepVerifier
                .create(memoCommentRepository.findById(targetComment.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNotNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteComment: 댓글이 남아있으면 메모는 삭제되지 않는다")
    void deleteComment_notLastComment() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .positions("{}")
                .build()).block();

        MemoComment comment1 = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("댓글 1")
                .build()).block();

        String comment2AuthorId = "06D6W8HDY79QFZX39RMX62KSX4";
        MemoComment comment2 = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(comment2AuthorId)
                .body("댓글 2")
                .build()).block();

        AuthenticatedUser comment2Author = AuthenticatedUser
                .of(comment2AuthorId);

        // reply(comment2) 삭제 -> 본문(comment1)은 살아있음
        StepVerifier
                .create(memoService.deleteComment(memo.getId(),
                        comment2.getId(), comment2Author))
                .verifyComplete();

        // comment2 삭제 확인
        StepVerifier.create(memoCommentRepository.findById(comment2.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNotNull())
                .verifyComplete();

        // comment1 생존 확인 (본문 유지)
        StepVerifier.create(memoCommentRepository.findById(comment1.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNull())
                .verifyComplete();

        // 메모 생존 확인
        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(m -> assertThat(m.getDeletedAt()).isNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteComment: 첫 댓글 삭제 시 메모만 삭제된다 (댓글은 유지)")
    void deleteComment_firstCommentDeletesMemo() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{}")
                .build()).block();

        MemoComment firstComment = memoCommentRepository.save(
                MemoComment.builder()
                        .memoId(memo.getId())
                        .authorId(authorId)
                        .body("본문")
                        .build())
                .block();

        MemoComment reply = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("06D6W8HDY79QFZX39RMX62KSX4")
                .body("답글")
                .build()).block();

        AuthenticatedUser user = AuthenticatedUser.of(authorId);

        StepVerifier.create(
                memoService.deleteComment(memo.getId(), firstComment.getId(),
                        user))
                .verifyComplete();

        // 메모 삭제 확인
        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(m -> assertThat(m.getDeletedAt()).isNotNull())
                .verifyComplete();

        // 첫 댓글은 삭제되지 않음 (상위 계층 삭제 시 하위 계층 유지 정책)
        StepVerifier
                .create(memoCommentRepository.findById(firstComment.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNull())
                .verifyComplete();

        // 나머지 댓글도 삭제되지 않음
        StepVerifier.create(memoCommentRepository.findById(reply.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteComment: 경로 memoId와 댓글 memoId가 다르면 INVALID_PARAMETER를 반환한다")
    void deleteComment_memoMismatch() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{}")
                .build()).block();

        MemoComment comment = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("댓글")
                .build()).block();

        AuthenticatedUser user = AuthenticatedUser.of(authorId);

        StepVerifier
                .create(memoService.deleteComment("06D6W1GAHD51T5NJPK29Q6BCR9",
                        comment.getId(), user))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.COMMON_INVALID_PARAMETER)
                .verify();
    }

    @Test
    @DisplayName("getMemo: 댓글을 생성 순서대로 정렬하여 반환한다")
    void getMemo_sortedComments() {
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .positions("{}")
                .build()).block();

        MemoComment first = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .body("본문")
                .build()).block();

        MemoComment second = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("06D6W8HDY79QFZX39RMX62KSX4")
                .body("답글")
                .build()).block();

        StepVerifier.create(memoService.getMemo(memo.getId()))
                .assertNext(response -> {
                    assertThat(response.getComments()).hasSize(2);
                    assertThat(response.getComments().get(0).getId())
                            .isEqualTo(first.getId());
                    assertThat(response.getComments().get(1).getId())
                            .isEqualTo(second.getId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getMemo: 존재하지 않는 메모를 조회하면 NOT_FOUND를 반환한다")
    void getMemo_notFound() {
        StepVerifier
                .create(memoService.getMemo("06D6W1GAHD51T5NJPK29Q6BCR9"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_MEMO_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("createComment: 존재하지 않는 메모에 댓글 생성 시 NOT_FOUND를 반환한다")
    void createComment_memoNotFound() {
        CreateMemoCommentRequest request = new CreateMemoCommentRequest("댓글");
        AuthenticatedUser user = AuthenticatedUser
                .of("01ARZ3NDEKTSV4RRFFQ69G5FAV");

        StepVerifier
                .create(memoService.createComment(
                        "06D6W1GAHD51T5NJPK29Q6BCR9", request, user))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_MEMO_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("getComments: 존재하지 않는 메모의 댓글 조회 시 NOT_FOUND를 반환한다")
    void getComments_memoNotFound() {
        StepVerifier
                .create(memoService.getComments("06D6W1GAHD51T5NJPK29Q6BCR9")
                        .collectList())
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_MEMO_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("updateComment: 작성자가 아니면 ACCESS_DENIED를 반환한다")
    void updateComment_accessDenied() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{}")
                .build()).block();

        MemoComment comment = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("원본 내용")
                .build()).block();

        UpdateMemoCommentRequest request = new UpdateMemoCommentRequest(
                memo.getId(), comment.getId(), "수정 내용");
        AuthenticatedUser otherUser = AuthenticatedUser
                .of("06D6W8HDY79QFZX39RMX62KSX4");

        StepVerifier.create(memoService.updateComment(request, otherUser))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ACCESS_DENIED)
                .verify();
    }

    @Test
    @DisplayName("deleteComment: 작성자가 아닌 일반 사용자는 ACCESS_DENIED를 반환한다")
    void deleteComment_accessDenied() {
        String authorId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("06D6VZBWHSDJBBG0H7D156YZ98")
                .authorId(authorId)
                .positions("{}")
                .build()).block();

        // 첫 번째 댓글 (본문)
        memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("본문")
                .build()).block();

        // 두 번째 댓글 (삭제 대상)
        MemoComment targetComment = memoCommentRepository
                .save(MemoComment.builder()
                        .memoId(memo.getId())
                        .authorId(authorId)
                        .body("댓글")
                        .build())
                .block();

        // 다른 일반 사용자 (OWNER/ADMIN 아님)
        AuthenticatedUser otherUser = AuthenticatedUser
                .of("06D6W8HDY79QFZX39RMX62KSX4");

        StepVerifier
                .create(memoService.deleteComment(memo.getId(),
                        targetComment.getId(), otherUser))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ACCESS_DENIED)
                .verify();
    }

}
