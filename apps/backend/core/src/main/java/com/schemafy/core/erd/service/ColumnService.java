package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.repository.ColumnRepository;
import com.schemafy.core.erd.repository.entity.Column;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ColumnService {

    private final ColumnRepository columnRepository;

    public Mono<Column> createColumn(Column column) {
        return columnRepository.save(column);
    }

    public Mono<Column> getColumn(String id) {
        return columnRepository.findById(id);
    }

    public Flux<Column> getColumnsByTableId(String tableId) {
        return columnRepository.findByTableId(tableId);
    }

    public Mono<Column> changeColumnName(Column column, String name) {
        column.setName(name);
        return columnRepository.save(column);
    }

    public Mono<Column> changeColumnType(Column column, String type) {
        column.setDataType(type);
        return columnRepository.save(column);
    }

    public Mono<Column> changeColumnPosition(Column column, int position) {
        column.setOrdinalPosition(position);
        return columnRepository.save(column);
    }

    public Mono<Column> changeColumnNullable(Column column,
            boolean nullable) {
        column.setNullable(nullable);
        return columnRepository.save(column);
    }

    public Mono<Void> deleteColumn(Column column) {
        column.delete();
        return columnRepository.save(column).then();
    }

}
