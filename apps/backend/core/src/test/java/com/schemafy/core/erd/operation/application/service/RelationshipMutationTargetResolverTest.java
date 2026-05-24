package com.schemafy.core.erd.operation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RelationshipMutationTargetResolverTest {

  @Mock
  ErdMutationTargetLookup targetLookup;

  RelationshipMutationTargetResolver sut;

  @BeforeEach
  void setUp() {
    sut = new RelationshipMutationTargetResolver(targetLookup);
  }

  @Test
  @DisplayName("CREATE_RELATIONSHIP의 FK 테이블이 없으면 relationship error로 변환한다")
  void mapsMissingCreateRelationshipFkTableToRelationshipError() {
    var command = new CreateRelationshipCommand(
        "missing-fk",
        "pk-table",
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null);

    given(targetLookup.resolveTableContext("missing-fk"))
        .willReturn(Mono.error(new DomainException(
            TableErrorCode.NOT_FOUND,
            "Table not found: missing-fk")));

    StepVerifier.create(sut.resolve(ErdOperationType.CREATE_RELATIONSHIP, command))
        .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND))
        .verify();
  }

}
