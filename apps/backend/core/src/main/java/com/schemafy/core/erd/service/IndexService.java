package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.IndexColumnRepository;
import com.schemafy.core.erd.repository.IndexRepository;
import com.schemafy.core.erd.repository.entity.Index;
import com.schemafy.core.erd.repository.entity.IndexColumn;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@Service
@RequiredArgsConstructor
public class IndexService {

    private final ValidationClient validationClient;
    private final IndexRepository indexRepository;
    private final IndexColumnRepository indexColumnRepository;

    public Mono<AffectedMappingResponse> createIndex(
            Validation.CreateIndexRequest request) {
        return validationClient.createIndex(request)
                .flatMap(database -> indexRepository
                        .save(ErdMapper.toEntity(request.getIndex()))
                        .map(savedIndex -> AffectedMappingResponse.of(
                                request,
                                request.getDatabase(),
                                AffectedMappingResponse.updateEntityIdInDatabase(
                                        database,
                                        EntityType.INDEX,
                                        request.getIndex().getId(),
                                        savedIndex.getId()
                                )
                        )));
    }

    public Mono<Index> getIndex(String id) {
        return indexRepository.findById(id);
    }

    public Flux<Index> getIndexesByTableId(String tableId) {
        return indexRepository.findByTableId(tableId);
    }

    public Mono<Index> updateIndexName(
            Validation.ChangeIndexNameRequest request) {
        return indexRepository
                .findById(request.getIndexId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_INDEX_NOT_FOUND)))
                .delayUntil(ignore -> validationClient.changeIndexName(request))
                .doOnNext(index -> index.setName(request.getNewName()))
                .flatMap(indexRepository::save);
    }

    public Mono<IndexColumn> addColumnToIndex(
            Validation.AddColumnToIndexRequest request) {
        return validationClient.addColumnToIndex(request)
                .then(indexColumnRepository
                        .save(ErdMapper.toEntity(request.getIndexColumn())));
    }

    public Mono<Void> removeColumnFromIndex(
            Validation.RemoveColumnFromIndexRequest request) {
        return indexColumnRepository
                .findById(request.getIndexColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_INDEX_NOT_FOUND)))
                .delayUntil(ignore -> validationClient
                        .removeColumnFromIndex(request))
                .doOnNext(IndexColumn::delete)
                .flatMap(indexColumnRepository::save)
                .then();
    }

    public Mono<Void> deleteIndex(
            Validation.DeleteIndexRequest request) {
        return indexRepository.findById(request.getIndexId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_INDEX_NOT_FOUND)))
                .delayUntil(ignore -> validationClient.deleteIndex(request))
                .doOnNext(Index::delete)
                .flatMap(indexRepository::save)
                .then();
    }

}
