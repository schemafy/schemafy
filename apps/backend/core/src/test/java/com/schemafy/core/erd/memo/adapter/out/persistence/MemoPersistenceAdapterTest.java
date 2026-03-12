package com.schemafy.core.erd.memo.adapter.out.persistence;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.config.R2dbcTestConfiguration;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.erd.memo.domain.MemoComment;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
  MemoPersistenceAdapter.class,
  MemoCommentPersistenceAdapter.class,
  MemoMapper.class,
  MemoCommentMapper.class,
  R2dbcTestConfiguration.class })
@DisplayName("MemoPersistenceAdapter")
class MemoPersistenceAdapterTest {

  @Autowired
  MemoPersistenceAdapter memoPersistenceAdapter;

  @Autowired
  MemoCommentPersistenceAdapter memoCommentPersistenceAdapter;

  @Autowired
  MemoRepository memoRepository;

  @Autowired
  MemoCommentRepository memoCommentRepository;

  @BeforeEach
  void setUp() {
    memoCommentRepository.deleteAll().block();
    memoRepository.deleteAll().block();
  }

  @Test
  @DisplayName("changeMemoPosition: 메모 위치를 변경한다")
  void changeMemoPosition_updatesPosition() {
    Memo memo = new Memo(
        "06D8A000000000000000000001",
        "schema-1",
        "author-1",
        "{}",
        null,
        null,
        null);

    memoPersistenceAdapter.createMemo(memo).block();

    StepVerifier.create(
        memoPersistenceAdapter.changeMemoPosition(memo.id(), "{\"x\":10}"))
        .verifyComplete();

    StepVerifier.create(memoPersistenceAdapter.findMemoById(memo.id()))
        .assertNext(found -> assertThat(found.positions()).isEqualTo(
            "{\"x\":10}"))
        .verifyComplete();
  }

  @Test
  @DisplayName("softDeleteMemo: soft delete된 메모는 조회되지 않는다")
  void softDeleteMemo_excludesDeletedMemo() {
    Memo memo = new Memo(
        "06D8A000000000000000000010",
        "schema-1",
        "author-1",
        "{}",
        null,
        null,
        null);

    memoPersistenceAdapter.createMemo(memo).block();

    StepVerifier.create(
        memoPersistenceAdapter.softDeleteMemo(memo.id(), Instant.now()))
        .verifyComplete();

    StepVerifier.create(memoPersistenceAdapter.findMemoById(memo.id()))
        .verifyComplete();
  }

  @Test
  @DisplayName("findMemoCommentsByMemoId: id ASC 정렬을 보장한다")
  void findMemoCommentsByMemoId_ordersByIdAsc() {
    Memo memo = new Memo(
        "06D8A000000000000000000020",
        "schema-1",
        "author-1",
        "{}",
        null,
        null,
        null);
    memoPersistenceAdapter.createMemo(memo).block();

    MemoComment later = new MemoComment(
        "06D8A000000000000000000022",
        memo.id(),
        "author-2",
        "later",
        null,
        null,
        null);
    MemoComment first = new MemoComment(
        "06D8A000000000000000000021",
        memo.id(),
        "author-1",
        "first",
        null,
        null,
        null);

    Flux.concat(
        memoCommentPersistenceAdapter.createMemoComment(later),
        memoCommentPersistenceAdapter.createMemoComment(first))
        .collectList()
        .block();

    StepVerifier.create(
        memoCommentPersistenceAdapter.findMemoCommentsByMemoId(memo.id())
            .collectList())
        .assertNext(comments -> {
          assertThat(comments).hasSize(2);
          assertThat(comments.get(0).id()).isEqualTo(first.id());
          assertThat(comments.get(1).id()).isEqualTo(later.id());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("changeMemoCommentBody: 댓글 본문을 변경한다")
  void changeMemoCommentBody_updatesBody() {
    Memo memo = new Memo(
        "06D8A000000000000000000030",
        "schema-1",
        "author-1",
        "{}",
        null,
        null,
        null);
    memoPersistenceAdapter.createMemo(memo).block();

    MemoComment comment = new MemoComment(
        "06D8A000000000000000000031",
        memo.id(),
        "author-1",
        "old",
        null,
        null,
        null);
    memoCommentPersistenceAdapter.createMemoComment(comment).block();

    StepVerifier.create(memoCommentPersistenceAdapter.changeMemoCommentBody(
        comment.id(), "new"))
        .verifyComplete();

    StepVerifier.create(memoCommentPersistenceAdapter.findMemoCommentById(
        comment.id()))
        .assertNext(found -> assertThat(found.body()).isEqualTo("new"))
        .verifyComplete();
  }

  @Test
  @DisplayName("softDeleteMemoComment: soft delete된 댓글은 목록 조회에서 제외한다")
  void softDeleteMemoComment_excludesDeletedComments() {
    Memo memo = new Memo(
        "06D8A000000000000000000040",
        "schema-1",
        "author-1",
        "{}",
        null,
        null,
        null);
    memoPersistenceAdapter.createMemo(memo).block();

    MemoComment active = new MemoComment(
        "06D8A000000000000000000041",
        memo.id(),
        "author-1",
        "active",
        null,
        null,
        null);
    MemoComment deleted = new MemoComment(
        "06D8A000000000000000000042",
        memo.id(),
        "author-2",
        "deleted",
        null,
        null,
        null);

    memoCommentPersistenceAdapter.createMemoComment(active).block();
    memoCommentPersistenceAdapter.createMemoComment(deleted).block();

    StepVerifier.create(memoCommentPersistenceAdapter.softDeleteMemoComment(
        deleted.id(), Instant.now()))
        .verifyComplete();

    StepVerifier.create(
        memoCommentPersistenceAdapter.findMemoCommentsByMemoId(memo.id())
            .collectList())
        .assertNext(comments -> {
          assertThat(comments).hasSize(1);
          assertThat(comments.get(0).id()).isEqualTo(active.id());
        })
        .verifyComplete();
  }

}
