package com.schemafy.domain.erd.memo.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.memo.domain.MemoComment;

@Component
class MemoCommentMapper {

  MemoCommentEntity toEntity(MemoComment comment) {
    return new MemoCommentEntity(
        comment.id(),
        comment.memoId(),
        comment.authorId(),
        comment.body(),
        comment.createdAt(),
        comment.updatedAt(),
        comment.deletedAt());
  }

  MemoComment toDomain(MemoCommentEntity entity) {
    return new MemoComment(
        entity.getId(),
        entity.getMemoId(),
        entity.getAuthorId(),
        entity.getBody(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt());
  }

}
