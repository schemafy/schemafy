package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Constraint;

public interface ConstraintRepository
        extends ReactiveCrudRepository<Constraint, String> {

}
