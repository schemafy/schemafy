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
@Table("memos")
public class Memo extends BaseEntity {

  @Column("schema_id")
  private String schemaId;

  @Column("author_id")
  private String authorId;

  @Column("positions")
  private String positions;

  @Builder(builderMethodName = "builder", buildMethodName = "build")
  private static Memo newMemo(String schemaId, String authorId,
      String positions) {
    Memo memo = new Memo(schemaId, authorId, positions);
    memo.setId(UlidGenerator.generate());
    return memo;
  }

}
