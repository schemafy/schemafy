package com.schemafy.core.erd.operation.adapter.out.persistence;

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
@Table("schema_collaboration_state")
public class SchemaCollaborationStateEntity implements Persistable<String> {

  @Id
  @Column("schema_id")
  private String schemaId;

  @Column("project_id")
  private String projectId;

  @Column("current_revision")
  private Long currentRevision;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @Override
  public String getId() {
    return schemaId;
  }

  @Override
  public boolean isNew() {
    return createdAt == null;
  }

}
