package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.erd.repository.entity.Constraint;
import com.schemafy.core.erd.repository.entity.ConstraintColumn;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class ConstraintService {

    private final ConstraintRepository constraintRepository;
    private final ConstraintColumnRepository constraintColumnRepository;

    public Mono<Constraint> createConstraint(Constraint constraint) {
        return constraintRepository.save(constraint);
    }

    public Mono<Constraint> getConstraint(String id) {
        return constraintRepository.findById(id);
    }

    public Flux<Constraint> getConstraintsByTableId(String tableId) {
        return constraintRepository.findByTableId(tableId);
    }

    public Mono<Constraint> changeConstraintName(Constraint constraint,
            String name) {
        constraint.setName(name);
        return constraintRepository.save(constraint);
    }

    public Mono<Constraint> addColumnToConstraint(Constraint constraint,
            ConstraintColumn constraintColumn) {
        String constraintId = constraint.getId();
        constraintColumn.setConstraintId(constraintId);
        constraintColumnRepository.save(constraintColumn);
        return constraintRepository.save(constraint);
    }

    public Mono<Void> removeColumnFromConstraint(
            ConstraintColumn constraintColumn) {
        constraintColumn.delete();
        return constraintColumnRepository.save(constraintColumn).then();
    }

    public Mono<Void> deleteConstraint(Constraint constraint) {
        constraint.delete();
        return constraintRepository.save(constraint).then();
    }

}
