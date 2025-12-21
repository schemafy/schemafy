package com.schemafy.core.erd.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
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
    private final TransactionalOperator transactionalOperator;

    public Mono<AffectedMappingResponse> createIndex(
            Validation.CreateIndexRequest request) {
        return validationClient.createIndex(request)
                .flatMap(database -> saveIndexWithColumns(request, database));
    }

    private Mono<AffectedMappingResponse> saveIndexWithColumns(
            Validation.CreateIndexRequest request,
            Validation.Database database) {
        return transactionalOperator.transactional(
                indexRepository
                        .save(ErdMapper.toEntity(request.getIndex()))
                        .flatMap(savedIndex -> {
                            return saveIndexColumns(
                                    request.getIndex().getColumnsList(),
                                    savedIndex.getId())
                                    .map(savedIndexColumnIdByColumnId -> {
                                        String validatorIndexId = findValidatorIndexId(
                                                database,
                                                request.getIndex());
                                        Map<String, String> indexColumnIdMappings = buildIndexColumnIdMappings(
                                                database,
                                                request.getIndex(),
                                                savedIndexColumnIdByColumnId);

                                        IdMappings idMappings = new IdMappings(
                                                Map.of(),
                                                Map.of(),
                                                Map.of(),
                                                Map.of(validatorIndexId,
                                                        savedIndex.getId()),
                                                indexColumnIdMappings,
                                                Map.of(),
                                                Map.of(),
                                                Map.of(),
                                                Map.of());

                                        Validation.Database rewrittenAfterDatabase = ValidationDatabaseIdRewriter
                                                .rewrite(database, idMappings);

                                        return AffectedMappingResponse.of(
                                                request,
                                                request.getDatabase(),
                                                rewrittenAfterDatabase);
                                    });
                        }));
    }

    private static String findValidatorIndexId(
            Validation.Database database,
            Validation.Index requestIndex) {
        for (Validation.Schema schema : database.getSchemasList()) {
            for (Validation.Table table : schema.getTablesList()) {
                if (!table.getId().equals(requestIndex.getTableId())) {
                    continue;
                }
                for (Validation.Index index : table.getIndexesList()) {
                    if (index.getName().equals(requestIndex.getName())) {
                        return index.getId();
                    }
                }
            }
        }
        return requestIndex.getId();
    }

    private static Map<String, String> buildIndexColumnIdMappings(
            Validation.Database database,
            Validation.Index requestIndex,
            Map<String, String> savedIndexColumnIdByColumnId) {
        Map<String, String> result = new HashMap<>();

        boolean foundIndex = false;
        for (Validation.Schema schema : database.getSchemasList()) {
            for (Validation.Table table : schema.getTablesList()) {
                if (!table.getId().equals(requestIndex.getTableId())) {
                    continue;
                }
                for (Validation.Index index : table.getIndexesList()) {
                    if (!index.getName().equals(requestIndex.getName())) {
                        continue;
                    }

                    foundIndex = true;
                    for (Validation.IndexColumn validatorIndexColumn : index
                            .getColumnsList()) {
                        String savedId = savedIndexColumnIdByColumnId
                                .get(validatorIndexColumn.getColumnId());
                        if (savedId != null) {
                            result.put(validatorIndexColumn.getId(), savedId);
                        }
                    }
                }
            }
        }

        if (foundIndex) {
            return result;
        }

        for (Validation.IndexColumn requestIndexColumn : requestIndex
                .getColumnsList()) {
            String savedId = savedIndexColumnIdByColumnId
                    .get(requestIndexColumn.getColumnId());
            if (savedId != null) {
                result.put(requestIndexColumn.getId(), savedId);
            }
        }
        return result;
    }

    private Mono<Map<String, String>> saveIndexColumns(
            List<Validation.IndexColumn> columns,
            String indexId) {
        return Flux.fromIterable(columns)
                .flatMap(column -> {
                    IndexColumn entity = ErdMapper.toEntity(column);
                    entity.setIndexId(indexId);
                    return indexColumnRepository.save(entity)
                            .map(saved -> Map.entry(column.getColumnId(),
                                    saved.getId()));
                })
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
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
                .delayUntil(
                        ignore -> validationClient.removeColumnFromIndex(
                                request))
                .flatMap(indexColumn -> transactionalOperator.transactional(
                        Mono.just(indexColumn)
                                .doOnNext(IndexColumn::delete)
                                .flatMap(indexColumnRepository::save)
                                .then(indexColumnRepository
                                        .findByIndexIdAndDeletedAtIsNull(
                                                request.getIndexId())
                                        .hasElements())
                                .flatMap(
                                        hasRemainingColumns -> hasRemainingColumns
                                                ? Mono.empty()
                                                : softDeleteIndexIfExists(
                                                        request.getIndexId()))))
                .then();
    }

    private Mono<Void> softDeleteIndexIfExists(String indexId) {
        return indexRepository.findByIdAndDeletedAtIsNull(indexId)
                .doOnNext(Index::delete)
                .flatMap(indexRepository::save)
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
