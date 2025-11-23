package com.schemafy.core.erd.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.core.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.core.erd.controller.dto.response.MemoDetailResponse;
import com.schemafy.core.erd.controller.dto.response.MemoResponse;
import com.schemafy.core.erd.repository.MemoCommentRepository;
import com.schemafy.core.erd.repository.MemoRepository;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.entity.Memo;
import com.schemafy.core.erd.repository.entity.MemoComment;
import com.schemafy.core.erd.repository.entity.Schema;

import reactor.core.publisher.Flux;
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
                .projectId("project-1")
                .dbVendorId("MYSQL")
                .name("test-schema")
                .build()).block();

        CreateMemoRequest request = new CreateMemoRequest(
                schema.getId(),
                "{\"x\":100,\"y\":100}",
                "초기 메모 내용");
        String authorId = "user-1";

        Mono<MemoDetailResponse> result = memoService.createMemo(request, authorId);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getSchemaId()).isEqualTo(schema.getId());
                    assertThat(response.getAuthorId()).isEqualTo(authorId);
                    assertThat(response.getPositions()).isEqualTo(request.positions());
                    assertThat(response.getComments()).hasSize(1);
                    assertThat(response.getComments().get(0).getBody()).isEqualTo(request.body());
                    assertThat(response.getComments().get(0).getAuthorId()).isEqualTo(authorId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createMemo: 스키마가 없으면 에러를 반환한다")
    void createMemo_schemaNotFound() {
        CreateMemoRequest request = new CreateMemoRequest(
                "non-existent-schema",
                "{\"x\":100,\"y\":100}",
                "내용");

        StepVerifier.create(memoService.createMemo(request, "user-1"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode() == ErrorCode.ERD_SCHEMA_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("getMemo: 메모 상세 정보를 조회한다")
    void getMemo_success() {
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("schema-1")
                .authorId("user-1")
                .positions("{}")
                .build()).block();

        memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("user-1")
                .body("댓글 1")
                .build()).block();

        memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("user-2")
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
        String schemaId = "schema-1";
        memoRepository.save(Memo.builder()
                .schemaId(schemaId)
                .authorId("user-1")
                .positions("{}")
                .build()).block();
        memoRepository.save(Memo.builder()
                .schemaId(schemaId)
                .authorId("user-2")
                .positions("{}")
                .build()).block();
        memoRepository.save(Memo.builder()
                .schemaId("other-schema")
                .authorId("user-1")
                .positions("{}")
                .build()).block();

        StepVerifier.create(memoService.getMemosBySchemaId(schemaId).collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateMemo: 작성자 본인이면 메모 위치를 수정한다")
    void updateMemo_success() {
        String authorId = "user-1";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("schema-1")
                .authorId(authorId)
                .positions("{\"x\":0,\"y\":0}")
                .build()).block();

        UpdateMemoRequest request = new UpdateMemoRequest(memo.getId(), "{\"x\":100,\"y\":100}");

        StepVerifier.create(memoService.updateMemo(request, authorId))
                .assertNext(response -> {
                    assertThat(response.getPositions()).isEqualTo(request.positions());
                })
                .verifyComplete();

        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(updated -> {
                    assertThat(updated.getPositions()).isEqualTo(request.positions());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateMemo: 작성자가 아니면 권한 에러를 반환한다")
    void updateMemo_accessDenied() {
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("schema-1")
                .authorId("user-1")
                .positions("{\"x\":0,\"y\":0}")
                .build()).block();

        UpdateMemoRequest request = new UpdateMemoRequest(memo.getId(), "{\"x\":100,\"y\":100}");

        StepVerifier.create(memoService.updateMemo(request, "user-2"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode() == ErrorCode.ACCESS_DENIED)
                .verify();
    }

    @Test
    @DisplayName("deleteMemo: 작성자 본인이면 메모와 관련 댓글을 모두 삭제한다 (Soft Delete)")
    void deleteMemo_success() {
        String authorId = "user-1";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("schema-1")
                .authorId(authorId)
                .positions("{}")
                .build()).block();

        MemoComment comment = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("user-2") // 댓글 작성자는 달라도 메모 작성자가 메모 삭제 시 같이 삭제됨
                .body("댓글")
                .build()).block();

        StepVerifier.create(memoService.deleteMemo(memo.getId(), authorId))
                .verifyComplete();

        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(m -> assertThat(m.getDeletedAt()).isNotNull())
                .verifyComplete();

        StepVerifier.create(memoCommentRepository.findById(comment.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNotNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("createComment: 메모 댓글을 생성한다")
    void createComment_success() {
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("schema-1")
                .authorId("user-1")
                .positions("{}")
                .build()).block();

        CreateMemoCommentRequest request = new CreateMemoCommentRequest("새 댓글");
        String authorId = "user-2";

        StepVerifier.create(memoService.createComment(memo.getId(), request, authorId))
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
        String authorId = "user-1";
        MemoComment comment = memoCommentRepository.save(MemoComment.builder()
                .memoId("memo-1")
                .authorId(authorId)
                .body("이전 내용")
                .build()).block();

        UpdateMemoCommentRequest request = new UpdateMemoCommentRequest(
                "memo-1", comment.getId(), "수정 내용");

        StepVerifier.create(memoService.updateComment(request, authorId))
                .assertNext(response -> {
                    assertThat(response.getBody()).isEqualTo("수정 내용");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteComment: 댓글 삭제 시 마지막 댓글이면 메모도 함께 삭제된다")
    void deleteComment_lastCommentCascading() {
        String authorId = "user-1";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("schema-1")
                .authorId("user-1") // 메모 작성자
                .positions("{}")
                .build()).block();

        MemoComment comment = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("유일한 댓글")
                .build()).block();

        StepVerifier.create(memoService.deleteComment(comment.getId(), authorId))
                .verifyComplete();

        // 댓글 삭제 확인
        StepVerifier.create(memoCommentRepository.findById(comment.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNotNull())
                .verifyComplete();

        // 메모 삭제 확인 (댓글이 하나도 없게 되었으므로)
        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(m -> assertThat(m.getDeletedAt()).isNotNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteComment: 댓글이 남아있으면 메모는 삭제되지 않는다")
    void deleteComment_notLastComment() {
        String authorId = "user-1";
        Memo memo = memoRepository.save(Memo.builder()
                .schemaId("schema-1")
                .authorId("user-1")
                .positions("{}")
                .build()).block();

        MemoComment comment1 = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId(authorId)
                .body("댓글 1")
                .build()).block();

        MemoComment comment2 = memoCommentRepository.save(MemoComment.builder()
                .memoId(memo.getId())
                .authorId("user-2")
                .body("댓글 2")
                .build()).block();

        // comment1 삭제
        StepVerifier.create(memoService.deleteComment(comment1.getId(), authorId))
                .verifyComplete();

        // comment1 삭제 확인
        StepVerifier.create(memoCommentRepository.findById(comment1.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNotNull())
                .verifyComplete();
        
        // comment2 생존 확인
        StepVerifier.create(memoCommentRepository.findById(comment2.getId()))
                .assertNext(c -> assertThat(c.getDeletedAt()).isNull())
                .verifyComplete();

        // 메모 생존 확인
        StepVerifier.create(memoRepository.findById(memo.getId()))
                .assertNext(m -> assertThat(m.getDeletedAt()).isNull())
                .verifyComplete();
    }
}
