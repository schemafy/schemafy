package com.schemafy.core.common.type;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

@Getter
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class BaseEntity implements Persistable<String> {

    @Id
    @Setter(lombok.AccessLevel.PROTECTED)
    protected String id;

    @CreatedDate
    protected Instant createdAt;

    @LastModifiedDate
    protected Instant updatedAt;

    protected Instant deletedAt;

    @Override
    public String getId() { return id; }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return this.createdAt == null;
    }

    public void delete() {
        this.deletedAt = Instant.now();
    }

    @JsonIgnore
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
