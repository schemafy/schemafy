package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("db_columns")
public class ColumnEntity implements Persistable<String> {

  @Id
  private String id;

  @Column("table_id")
  private String tableId;

  @Column("name")
  private String name;

  @Column("data_type")
  private String dataType;

  @Column("length_scale")
  private String lengthScale;

  @Column("seq_no")
  private int seqNo;

  @Column("auto_increment")
  private Boolean autoIncrement;

  @Column("charset")
  private String charset;

  @Column("collation")
  private String collation;

  @Column("comment")
  private String comment;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  private Instant deletedAt;

  protected ColumnEntity() {}

  ColumnEntity(String id, String tableId, String name, String dataType,
      String lengthScale, int seqNo, Boolean autoIncrement, String charset,
      String collation, String comment) {
    this.id = id;
    this.tableId = tableId;
    this.name = name;
    this.dataType = dataType;
    this.lengthScale = lengthScale;
    this.seqNo = seqNo;
    this.autoIncrement = autoIncrement;
    this.charset = charset;
    this.collation = collation;
    this.comment = comment;
  }

  @Override
  public boolean isNew() { return this.createdAt == null; }

  @Override
  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public String getLengthScale() {
    return lengthScale;
  }

  public void setLengthScale(String lengthScale) {
    this.lengthScale = lengthScale;
  }

  public int getSeqNo() {
    return seqNo;
  }

  public void setSeqNo(int seqNo) {
    this.seqNo = seqNo;
  }

  public Boolean getAutoIncrement() {
    return autoIncrement;
  }

  public void setAutoIncrement(Boolean autoIncrement) {
    this.autoIncrement = autoIncrement;
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public String getCollation() {
    return collation;
  }

  public void setCollation(String collation) {
    this.collation = collation;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

}
