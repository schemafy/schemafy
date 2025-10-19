package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.repository.TableRepository;
import com.schemafy.core.erd.repository.entity.Table;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;

    public Mono<Table> createTable(Table table) {
        return tableRepository.save(table);
    }

    public Mono<Table> getTable(String id) {
        return tableRepository.findById(id);
    }

    public Flux<Table> getTablesBySchemaId(String schemaId) {
        return tableRepository.findBySchemaId(schemaId);
    }

    public Mono<Table> changeTableName(Table table, String name) {
        table.setName(name);
        return tableRepository.save(table);
    }

    public Mono<Void> deleteTable(Table table) {
        table.delete();
        return tableRepository.save(table).then();
    }

}
