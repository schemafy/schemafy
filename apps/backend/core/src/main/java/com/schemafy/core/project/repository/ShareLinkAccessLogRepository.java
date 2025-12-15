package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.project.repository.entity.ShareLinkAccessLog;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ShareLinkAccessLogRepository
        extends ReactiveCrudRepository<ShareLinkAccessLog, String> {

    @Query("SELECT * FROM share_link_access_logs WHERE share_link_id = :shareLinkId ORDER BY accessed_at DESC LIMIT :limit OFFSET :offset")
    Flux<ShareLinkAccessLog> findByShareLinkId(String shareLinkId, int limit,
            int offset);

    @Query("SELECT COUNT(*) FROM share_link_access_logs WHERE share_link_id = :shareLinkId")
    Mono<Long> countByShareLinkId(String shareLinkId);

    @Query("SELECT COUNT(DISTINCT COALESCE(user_id, ip_address)) FROM share_link_access_logs WHERE share_link_id = :shareLinkId")
    Mono<Long> countUniqueAccessors(String shareLinkId);

    @Query("DELETE FROM share_link_access_logs WHERE share_link_id = :shareLinkId")
    Mono<Void> deleteByShareLinkId(String shareLinkId);

}
