package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.IndexColumnRepository;
import com.schemafy.core.erd.repository.IndexRepository;
import com.schemafy.core.erd.repository.entity.Index;
import com.schemafy.core.erd.repository.entity.IndexColumn;
import com.schemafy.core.validation.client.ValidationClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@Service
public class IndexService extends BaseErdService {

    private final ValidationClient validationClient;
    private final IndexRepository indexRepository;
    private final IndexColumnRepository indexColumnRepository;

    public IndexService(
            ValidationClient validationClient,
            IndexRepository indexRepository,
            IndexColumnRepository indexColumnRepository,
            TransactionalOperator transactionalOperator) {
        super(transactionalOperator);
        this.validationClient = validationClient;
        this.indexRepository = indexRepository;
        this.indexColumnRepository = indexColumnRepository;
    }

    public Mono<AffectedMappingResponse> createIndex(
            Validation.CreateIndexRequest request) {
        return validationClient.createIndex(request)
                .flatMap(database -> transactional(indexRepository
                        .save(ErdMapper.toEntity(request.getIndex()))
                        .flatMap(savedIndex -> Flux
                                .fromIterable(
                                        request.getIndex().getColumnsList())
                                .flatMap(column -> {
                                    IndexColumn entity = ErdMapper
                                            .toEntity(column);
                                    entity.setIndexId(savedIndex.getId());
                                    return indexColumnRepository
                                            .save(entity);
                                })
                                .then(Mono.just(AffectedMappingResponse.of(
                                        request,
                                        request.getDatabase(),
                                        AffectedMappingResponse
                                                .updateEntityIdInDatabase(
                                                        database,
                                                        EntityType.INDEX,
                                                        request.getIndex()
                                                                .getId(),
                                                        savedIndex
                                                                .getId())))))));
    }

    public Mono<IndexResponse> getIndex(String id) {
        return indexRepository.findByIdAndDeletedAtIsNull(id)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_INDEX_NOT_FOUND)))
                .flatMap(index -> indexColumnRepository
                        .findByIndexIdAndDeletedAtIsNull(id)
                        .collectList()
                        .map(columns -> IndexResponse.from(index, columns)));
    }

    public Flux<IndexResponse> getIndexesByTableId(String tableId) {
        return indexRepository.findByTableIdAndDeletedAtIsNull(tableId)
                .flatMap(index -> indexColumnRepository
                        .findByIndexIdAndDeletedAtIsNull(index.getId())
                        .collectList()
                        .map(columns -> IndexResponse.from(index, columns)));
    }

    public Mono<IndexResponse> updateIndexName(
            Validation.ChangeIndexNameRequest request) {
        return indexRepository
                .findByIdAndDeletedAtIsNull(request.getIndexId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_INDEX_NOT_FOUND)))
                .delayUntil(ignore -> validationClient.changeIndexName(request))
                .doOnNext(index -> index.setName(request.getNewName()))
                .flatMap(indexRepository::save)
                .map(IndexResponse::from);
    }

    public Mono<IndexColumnResponse> addColumnToIndex(
            Validation.AddColumnToIndexRequest request) {
        return validationClient.addColumnToIndex(request)
                .then(indexColumnRepository
                        .save(ErdMapper.toEntity(request.getIndexColumn())))
                .map(IndexColumnResponse::from);
    }

    public Mono<Void> removeColumnFromIndex(
            Validation.RemoveColumnFromIndexRequest request) {
        return indexColumnRepository
                .findByIdAndDeletedAtIsNull(request.getIndexColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_INDEX_COLUMN_NOT_FOUND)))
                .delayUntil(ignore -> validationClient
                        .removeColumnFromIndex(request))
                .doOnNext(IndexColumn::delete)
                .flatMap(indexColumnRepository::save)
                .then();
    }

    public Mono<Void> deleteIndex(
            Validation.DeleteIndexRequest request) {
        return indexRepository.findByIdAndDeletedAtIsNull(request.getIndexId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_INDEX_NOT_FOUND)))
                .delayUntil(ignore -> validationClient.deleteIndex(request))
                .doOnNext(Index::delete)
                .flatMap(indexRepository::save)
                .then();
    }

}
