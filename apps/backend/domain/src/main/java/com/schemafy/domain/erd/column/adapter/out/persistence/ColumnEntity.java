package com.schemafy.domain.erd.column.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
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

  @Version
  private Long version;

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
  public boolean isNew() { return this.version == null; }

  @Override
  public String getId() { return this.id; }

}
