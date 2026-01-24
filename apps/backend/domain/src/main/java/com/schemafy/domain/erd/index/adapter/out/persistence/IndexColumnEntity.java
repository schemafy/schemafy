package com.schemafy.domain.erd.index.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("db_index_columns")
public class IndexColumnEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("index_id")
  private String indexId;

  @Column("column_id")
  private String columnId;

  @Column("seq_no")
  private int seqNo;

  @Column("sort_dir")
  private String sortDirection;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  private Instant deletedAt;

  IndexColumnEntity(
      String id,
      String indexId,
      String columnId,
      int seqNo,
      String sortDirection) {
    this.id = id;
    this.indexId = indexId;
    this.columnId = columnId;
    this.seqNo = seqNo;
    this.sortDirection = sortDirection;
  }

  @Override
  public boolean isNew() { return this.createdAt == null; }

  @Override
  public String getId() { return this.id; }

  public String getIndexId() { return indexId; }

  public String getColumnId() { return columnId; }

  public int getSeqNo() { return seqNo; }

  public String getSortDirection() { return sortDirection; }

  public void setSeqNo(int seqNo) { this.seqNo = seqNo; }

  public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

  public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

}
