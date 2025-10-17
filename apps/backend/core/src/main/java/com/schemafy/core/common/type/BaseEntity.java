package com.schemafy.core.common.type;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class BaseEntity implements Persistable<String> {

    @Id
    @Setter(lombok.AccessLevel.PROTECTED)
    protected String id;

    @CreatedDate
    protected LocalDateTime createdAt;

    @LastModifiedDate
    protected LocalDateTime updatedAt;

    protected LocalDateTime deletedAt;

    @Transient
    protected boolean isNew = true;

    @Override
    public String getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    public void markAsNotNew() {
        this.isNew = false;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() { return deletedAt != null; }
}
