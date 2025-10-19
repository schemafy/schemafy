package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.repository.RelationshipColumnRepository;
import com.schemafy.core.erd.repository.RelationshipRepository;
import com.schemafy.core.erd.repository.entity.Relationship;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final RelationshipColumnRepository relationshipColumnRepository;

    public Mono<Relationship> createRelationship(Relationship relationship) {
        return relationshipRepository.save(relationship);
    }

    public Mono<Relationship> getRelationship(String id) {
        return relationshipRepository.findById(id);
    }

    public Flux<Relationship> getRelationshipsByTableId(String tableId) {
        return relationshipRepository.findByTableId(tableId);
    }

    public Mono<Relationship> changeRelationshipName(Relationship relationship,
            String name) {
        relationship.setName(name);
        return relationshipRepository.save(relationship);
    }

    public Mono<Relationship> changeRelationshipCardinality(
            Relationship relationship,
            String cardinality) {
        relationship.setCardinality(cardinality);
        return relationshipRepository.save(relationship);
    }

    public Mono<Relationship> addColumnToRelationship(Relationship relationship,
            RelationshipColumn relationshipColumn) {
        String relationshipId = relationship.getId();
        relationshipColumn.setRelationshipId(relationshipId);
        relationshipColumnRepository.save(relationshipColumn);
        return relationshipRepository.save(relationship);
    }

    public Mono<Void> removeColumnFromRelationship(
            RelationshipColumn relationshipColumn) {
        relationshipColumn.delete();
        return relationshipColumnRepository.save(relationshipColumn).then();
    }

    public Mono<Void> deleteRelationship(Relationship relationship) {
        relationship.delete();
        return relationshipRepository.save(relationship).then();
    }

}
