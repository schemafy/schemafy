package com.schemafy.core.project.repository.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("share_link_access_logs")
public class ShareLinkAccessLog implements Persistable<String> {

    @Id
    private String id;

    private String shareLinkId;

    private String userId;

    private String ipAddress;

    private String userAgent;

    @CreatedDate
    private Instant accessedAt;

    public static ShareLinkAccessLog create(String shareLinkId, String userId,
            String ipAddress, String userAgent) {
        return new ShareLinkAccessLog(
                UlidGenerator.generate(),
                shareLinkId,
                userId,
                ipAddress,
                userAgent,
                null);
    }

    @Override
    public String getId() { return id; }

    @Override
    @JsonIgnore
    public boolean isNew() { return this.accessedAt == null; }

    public boolean isAnonymousAccess() { return userId == null; }

}
