package com.schemafy.core.erd.repository.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("memo_comments")
public class MemoComment extends BaseEntity {

    @Column("memo_id")
    private String memoId;

    @Column("author_id")
    private String authorId;

    @Column("body")
    private String body;

    @Builder(builderMethodName = "builder", buildMethodName = "build")
    private static MemoComment newMemoComment(String memoId, String authorId,
            String body) {
        MemoComment memoComment = new MemoComment(memoId, authorId, body);
        memoComment.setId(UlidGenerator.generate());
        return memoComment;
    }

}
