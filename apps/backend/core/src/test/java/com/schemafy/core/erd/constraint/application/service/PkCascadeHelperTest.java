package com.schemafy.core.erd.constraint.application.service;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.core.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.core.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.core.erd.constraint.application.port.out.CreateConstraintPort;
import com.schemafy.core.erd.constraint.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.core.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("PkCascadeHelper")
class PkCascadeHelperTest {

  private static final String SCHEMA_ID = "schema-id";
  private static final String PK_TABLE_ID = "pk-table-id";
  private static final String FK_TABLE_ID = "fk-table-id";
  private static final String RELATIONSHIP_ID = "relationship-id";

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  ConstraintExistsPort constraintExistsPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Mock
  CreateColumnPort createColumnPort;

  @Mock
  DeleteColumnUseCase deleteColumnUseCase;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @Mock
  CreateConstraintPort createConstraintPort;

  @Mock
  CreateConstraintColumnPort createConstraintColumnPort;

  @Mock
  DeleteConstraintColumnPort deleteConstraintColumnPort;

  @Mock
  DeleteConstraintPort deleteConstraintPort;

  @Mock
  GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Mock
  CreateRelationshipColumnPort createRelationshipColumnPort;

  @Mock
  DeleteRelationshipColumnPort deleteRelationshipColumnPort;

  @Mock
  DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsByRelationshipIdPort;

  @Mock
  DeleteRelationshipPort deleteRelationshipPort;

  @Mock
  IdentifierCapabilityResolver identifierCapabilityResolver;

  @InjectMocks
  PkCascadeHelper sut;

  @Test
  @DisplayName("40자 PK 컬럼 이름이 충돌하면 suffix 공간을 확보해 FK 컬럼을 전파한다")
  void fitsCascadeFkColumnSuffixWithinLocalColumnLimit() {
    String sourceName = "c".repeat(40);
    var pkColumn = column("pk-column-id", PK_TABLE_ID, sourceName);
    var existingFkColumn = column("existing-fk-column-id", FK_TABLE_ID, sourceName);
    var relationship = new Relationship(
        RELATIONSHIP_ID,
        PK_TABLE_ID,
        FK_TABLE_ID,
        "relationship_name",
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null);

    given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(PK_TABLE_ID))
        .willReturn(Mono.just(List.of(relationship)));
    given(getColumnsByTableIdPort.findColumnsByTableId(FK_TABLE_ID))
        .willReturn(Mono.just(List.of(existingFkColumn)));
    given(getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(RELATIONSHIP_ID))
        .willReturn(Mono.just(List.of()));
    given(identifierCapabilityResolver.resolve(TABLE, FK_TABLE_ID))
        .willReturn(Mono.just(IdentifierCapabilities.codePoints(64)));
    given(ulidGeneratorPort.generate())
        .willReturn("fk-column-id", "relationship-column-id");
    given(createColumnPort.createColumn(any(Column.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(sut.cascadeAddPkColumn(
        PK_TABLE_ID,
        pkColumn,
        new HashSet<>(),
        new HashSet<>()))
        .assertNext(created -> {
          assertThat(created).hasSize(1);
          assertThat(created.get(0).fkColumnName())
              .isEqualTo("c".repeat(38) + "_1")
              .hasSize(40);
        })
        .verifyComplete();

    then(identifierCapabilityResolver).should().resolve(TABLE, FK_TABLE_ID);
  }

  @Test
  @DisplayName("긴 테이블의 자동 PK 제약 이름을 suffix까지 포함해 vendor 제한에 맞춘다")
  void fitsGeneratedPkConstraintNameWithinIdentifierLimit() {
    String tableName = "t".repeat(64);
    String firstCandidate = "pk_" + "t".repeat(61);
    String suffixedCandidate = "pk_" + "t".repeat(59) + "_1";
    var table = new Table(
        FK_TABLE_ID,
        SCHEMA_ID,
        tableName,
        "utf8mb4",
        "utf8mb4_general_ci");
    var fkColumn = column("fk-column-id", FK_TABLE_ID, "id");

    given(getConstraintsByTableIdPort.findConstraintsByTableId(FK_TABLE_ID))
        .willReturn(Mono.just(List.of()));
    given(getTableByIdPort.findTableById(FK_TABLE_ID))
        .willReturn(Mono.just(table));
    given(identifierCapabilityResolver.resolve(TABLE, FK_TABLE_ID))
        .willReturn(Mono.just(IdentifierCapabilities.codePoints(64)));
    given(constraintExistsPort.existsBySchemaIdAndName(SCHEMA_ID, firstCandidate))
        .willReturn(Mono.just(true));
    given(constraintExistsPort.existsBySchemaIdAndName(SCHEMA_ID, suffixedCandidate))
        .willReturn(Mono.just(false));
    given(ulidGeneratorPort.generate())
        .willReturn("pk-constraint-id", "pk-constraint-column-id");
    given(createConstraintPort.createConstraint(any(Constraint.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(getConstraintColumnsByConstraintIdPort
        .findConstraintColumnsByConstraintId("pk-constraint-id"))
        .willReturn(Mono.just(List.of()));
    given(createConstraintColumnPort.createConstraintColumn(any()))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(FK_TABLE_ID))
        .willReturn(Mono.just(List.of()));

    StepVerifier.create(sut.addPkColumnAndCascade(
        FK_TABLE_ID,
        fkColumn,
        new HashSet<>(),
        new HashSet<>()))
        .verifyComplete();

    ArgumentCaptor<Constraint> constraintCaptor = ArgumentCaptor.forClass(Constraint.class);
    then(createConstraintPort).should().createConstraint(constraintCaptor.capture());
    assertThat(constraintCaptor.getValue().name())
        .isEqualTo(suffixedCandidate)
        .hasSize(64)
        .endsWith("_1");
    then(identifierCapabilityResolver).should().resolve(TABLE, FK_TABLE_ID);
  }

  private Column column(String id, String tableId, String name) {
    return new Column(
        id,
        tableId,
        name,
        "INT",
        null,
        0,
        false,
        null,
        null,
        null);
  }

}
