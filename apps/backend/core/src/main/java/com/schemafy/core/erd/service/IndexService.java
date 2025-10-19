package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.repository.IndexColumnRepository;
import com.schemafy.core.erd.repository.IndexRepository;
import com.schemafy.core.erd.repository.entity.Index;
import com.schemafy.core.erd.repository.entity.IndexColumn;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class IndexService {

    private final IndexRepository indexRepository;
    private final IndexColumnRepository indexColumnRepository;

    public Mono<Index> createIndex(Index index) {
        return indexRepository.save(index);
    }

    public Mono<Index> getIndex(String id) {
        return indexRepository.findById(id);
    }

    public Flux<Index> getIndexesByTableId(String tableId) {
        return indexRepository.findByTableId(tableId);
    }

    public Mono<Index> changeIndexName(Index index, String name) {
        index.setName(name);
        return indexRepository.save(index);
    }

    public Mono<Index> addColumnToIndex(Index index, IndexColumn indexColumn) {
        String indexId = index.getId();
        indexColumn.setIndexId(indexId);
        indexColumnRepository.save(indexColumn);
        return indexRepository.save(index);
    }

    public Mono<Void> removeColumnFromIndex(
            IndexColumn indexColumn) {
        indexColumn.delete();
        return indexColumnRepository.save(indexColumn).then();
    }

    public Mono<Void> deleteIndex(Index index) {
        index.delete();
        return indexRepository.save(index).then();
    }

}
