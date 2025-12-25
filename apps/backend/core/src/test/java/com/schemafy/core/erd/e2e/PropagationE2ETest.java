package com.schemafy.core.erd.e2e;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.WithMockCustomUser;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.ColumnRepository;
import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.erd.repository.IndexColumnRepository;
import com.schemafy.core.erd.repository.IndexRepository;
import com.schemafy.core.erd.repository.RelationshipColumnRepository;
import com.schemafy.core.erd.repository.RelationshipRepository;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.TableRepository;

import validation.Validation;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DisplayName("Relationship propagation e2e")
@WithMockCustomUser(roles = "EDITOR")
class PropagationE2ETest {

    private static final String API_BASE_PATH = ApiPath.API
            .replace("{version}", "v1.0");
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private static final String DATABASE_ID = "06DM1EE3B0GWCZR6WX47EPMDNW";
    private static final String SCHEMA_REQUEST_ID = "06DM1EF33VJGYMTJ14CB2KW9E4";
    private static final String PARENT_TABLE_REQUEST_ID = "06DM1ERGQ1PAJ8XJVVCWANCTWC";
    private static final String CHILD_TABLE_REQUEST_ID = "06DM1EWQ8NR2TYSR60GY6YC9M0";
    private static final String PARENT_COLUMN_REQUEST_ID = "06DM1GXTA463ZRJQ9Z49TY5D04";
    private static final String CHILD_COLUMN_REQUEST_ID = "06DM1HFV32RT0EXAHPNSWZWJ0M";
    private static final String PK_REQUEST_ID = "06DM1HZK016R3YBN6WBF9TQJ8C";
    private static final String PK_COLUMN_REQUEST_ID = "06DM1J80MTQ0JDAY9ADJGDXVHR";
    private static final String RELATIONSHIP_REQUEST_ID = "06DM1ZF4PN20Q564N19VABPGPG";
    private static final String REL_COLUMN_REQUEST_ID = "06DM1ZWBFSJ98RXF6PPA3XKFPC";
    private static final String FK_COLUMN_REQUEST_ID = "06DM236JPDCG17FN6TC0ZNMFNC";
    private static final String EXTRA_PARENT_COLUMN_REQUEST_ID = "06DM1GXTA463ZRJQ9Z49TY5D05";
    private static final String EXTRA_PARENT_PK_COLUMN_REQUEST_ID = "06DM1J80MTQ0JDAY9ADJGDXVHS";
    private static final String REL_EXTRA_PARENT_COLUMN_REQUEST_ID = "06DM1GXTA463ZRJQ9Z49TY5D06";
    private static final String REL_EXTRA_CHILD_COLUMN_REQUEST_ID = "06DM1HFV32RT0EXAHPNSWZWJ0N";
    private static final String REL_EXTRA_REL_COLUMN_REQUEST_ID = "06DM1ZWBFSJ98RXF6PPA3XKFPE";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SchemaRepository schemaRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private IndexColumnRepository indexColumnRepository;

    @Autowired
    private ConstraintRepository constraintRepository;

    @Autowired
    private ConstraintColumnRepository constraintColumnRepository;

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private RelationshipColumnRepository relationshipColumnRepository;

    @BeforeEach
    void resetDatabase() {
        relationshipColumnRepository.deleteAll().block();
        relationshipRepository.deleteAll().block();
        constraintColumnRepository.deleteAll().block();
        constraintRepository.deleteAll().block();
        indexColumnRepository.deleteAll().block();
        indexRepository.deleteAll().block();
        columnRepository.deleteAll().block();
        tableRepository.deleteAll().block();
        schemaRepository.deleteAll().block();
    }

    @Test
    @DisplayName("IDENTIFYING 관계 생성 시 전파/매핑이 정상 반환된다")
    void identifyingRelationshipMappingFlow() throws Exception {
        SetupResult setup = setupIdentifyingRelationship();
        SetupState state = setup.state();
        JsonNode propagated = setup.relationshipResponse().path("result")
                .path("propagated");

        assertThat(state.relationshipId()).isNotBlank()
                .isNotEqualTo(RELATIONSHIP_REQUEST_ID);
        assertThat(state.relationshipColumnId()).isNotBlank()
                .isNotEqualTo(REL_COLUMN_REQUEST_ID);

        assertThat(propagated.path("columns")).hasSize(1);
        assertThat(propagated.path("constraints")).hasSize(1);
        assertThat(propagated.path("constraintColumns")).hasSize(1);
        assertThat(propagated.path("relationshipColumns")).isEmpty();
        assertThat(propagated.path("indexColumns")).isEmpty();

        assertThat(state.propagatedColumnId()).isNotBlank();
        assertThat(state.propagatedSourceType())
                .isEqualTo(EntityType.RELATIONSHIP.name());
        assertThat(state.propagatedSourceId())
                .isEqualTo(state.relationshipId());
        assertThat(state.propagatedSourceColumnId())
                .isEqualTo(state.parentColumnId());
        assertThat(state.propagatedConstraintId()).isNotBlank();
        assertThat(state.propagatedConstraintColumnId()).isNotBlank();
    }

    @Test
    @DisplayName("NON_IDENTIFYING 관계 생성 시 전파가 컬럼만 반환된다")
    void nonIdentifyingRelationshipMappingFlow() throws Exception {
        SetupResult setup = setupNonIdentifyingRelationship();
        SetupState state = setup.state();
        JsonNode propagated = setup.relationshipResponse().path("result")
                .path("propagated");

        assertThat(state.relationshipId()).isNotBlank()
                .isNotEqualTo(RELATIONSHIP_REQUEST_ID);
        assertThat(state.relationshipColumnId()).isNotBlank()
                .isNotEqualTo(REL_COLUMN_REQUEST_ID);

        assertThat(propagated.path("columns")).hasSize(1);
        assertThat(propagated.path("constraints")).isEmpty();
        assertThat(propagated.path("constraintColumns")).isEmpty();
        assertThat(propagated.path("relationshipColumns")).isEmpty();
        assertThat(propagated.path("indexColumns")).isEmpty();

        assertThat(state.propagatedConstraintId()).isEmpty();
        assertThat(state.propagatedConstraintColumnId()).isEmpty();
        assertThat(state.propagatedSourceType())
                .isEqualTo(EntityType.RELATIONSHIP.name());
        assertThat(state.propagatedSourceId())
                .isEqualTo(state.relationshipId());
        assertThat(state.propagatedSourceColumnId())
                .isEqualTo(state.parentColumnId());
    }

    @Test
    @DisplayName("관계 삭제 시 전파된 컬럼/제약조건이 제거된다")
    void deleteRelationshipRemovesPropagatedEntities() throws Exception {
        SetupState state = setupIdentifyingRelationship().state();

        Validation.Database database = buildDatabaseWithRelationship(state);
        Validation.DeleteRelationshipRequest deleteRequest = Validation.DeleteRelationshipRequest
                .newBuilder()
                .setDatabase(database)
                .setSchemaId(state.schemaId())
                .setRelationshipId(state.relationshipId())
                .build();

        JsonNode deleteResponse = deleteJson(
                "/relationships/" + state.relationshipId(),
                toJson(deleteRequest));
        assertThat(deleteResponse.path("success").asBoolean()).isTrue();

        JsonNode childTableResponse = getJson(
                "/tables/" + state.childTableId());
        JsonNode columns = childTableResponse.path("result")
                .path("columns");
        JsonNode constraints = childTableResponse.path("result")
                .path("constraints");
        JsonNode relationships = childTableResponse.path("result")
                .path("relationships");

        assertThat(containsId(columns, state.childColumnId())).isTrue();
        assertThat(containsId(columns, state.propagatedColumnId())).isFalse();
        assertThat(containsId(constraints, state.propagatedConstraintId()))
                .isFalse();
        assertThat(relationships).isEmpty();
    }

    @Test
    @DisplayName("NON_IDENTIFYING 관계 컬럼 추가 시 제약은 변경되지 않는다")
    void addRelationshipColumnDoesNotChangeConstraints() throws Exception {
        SetupState state = setupNonIdentifyingRelationship().state();

        Validation.Table parentTable = buildParentTableWithPk(state,
                List.of(buildColumn(state.parentColumnId(),
                        state.parentTableId(), "id", 1, true)));
        Validation.Table childTable = buildChildTableWithRelationship(state);

        JsonNode extraParentResponse = postJson("/columns",
                toJson(buildCreateColumnRequest(state.schemaId(),
                        state.parentTableId(),
                        REL_EXTRA_PARENT_COLUMN_REQUEST_ID, "code", 2, false,
                        List.of(parentTable, childTable))));
        String extraParentColumnId = extractNestedMapping(extraParentResponse,
                "columns", state.parentTableId(),
                REL_EXTRA_PARENT_COLUMN_REQUEST_ID);

        Validation.Table parentWithExtra = buildParentTableWithPk(state,
                List.of(buildColumn(state.parentColumnId(),
                        state.parentTableId(), "id", 1, true),
                        buildColumn(extraParentColumnId,
                                state.parentTableId(), "code", 2, false)));

        JsonNode extraChildResponse = postJson("/columns",
                toJson(buildCreateColumnRequest(state.schemaId(),
                        state.childTableId(),
                        REL_EXTRA_CHILD_COLUMN_REQUEST_ID, "parent_code", 3,
                        false,
                        List.of(parentWithExtra, childTable))));
        String extraChildColumnId = extractNestedMapping(extraChildResponse,
                "columns", state.childTableId(),
                REL_EXTRA_CHILD_COLUMN_REQUEST_ID);

        Validation.RelationshipColumn extraRelationshipColumn = buildRelationshipColumn(
                REL_EXTRA_REL_COLUMN_REQUEST_ID, state.relationshipId(),
                extraChildColumnId, extraParentColumnId, 2);
        Validation.Relationship existingRelationship = buildRelationship(state,
                List.of(buildRelationshipColumn(state.relationshipColumnId(),
                        state.relationshipId(), state.propagatedColumnId(),
                        state.parentColumnId(), 1)));

        Validation.Table childWithExtra = buildTable(state.childTableId(),
                state.schemaId(), "child",
                List.of(buildColumn(state.childColumnId(),
                        state.childTableId(), "id", 1, true),
                        buildColumn(state.propagatedColumnId(),
                                state.childTableId(), "parent_id", 2, false),
                        buildColumn(extraChildColumnId,
                                state.childTableId(), "parent_code", 3,
                                false)),
                List.of(), List.of(existingRelationship));

        Validation.AddColumnToRelationshipRequest addRequest = Validation.AddColumnToRelationshipRequest
                .newBuilder()
                .setDatabase(buildDatabase(state.schemaId(),
                        List.of(parentWithExtra, childWithExtra)))
                .setSchemaId(state.schemaId())
                .setRelationshipId(state.relationshipId())
                .setRelationshipColumn(extraRelationshipColumn)
                .build();

        JsonNode addResponse = postJson(
                "/relationships/" + state.relationshipId() + "/columns",
                toJson(addRequest));

        String mappedRelationshipColumnId = extractNestedMapping(addResponse,
                "relationshipColumns", state.relationshipId(),
                REL_EXTRA_REL_COLUMN_REQUEST_ID);
        assertThat(mappedRelationshipColumnId).isNotBlank()
                .isNotEqualTo(REL_EXTRA_REL_COLUMN_REQUEST_ID);

        JsonNode propagated = addResponse.path("result").path("propagated");
        assertThat(propagated.path("columns")).isEmpty();
        assertThat(propagated.path("constraints")).isEmpty();
        assertThat(propagated.path("constraintColumns")).isEmpty();
        assertThat(propagated.path("relationshipColumns")).isEmpty();
        assertThat(propagated.path("indexColumns")).isEmpty();

        JsonNode childTableResponse = getJson(
                "/tables/" + state.childTableId());
        JsonNode relationships = childTableResponse.path("result")
                .path("relationships");
        assertThat(relationshipColumnCount(relationships)).isEqualTo(2);
        assertThat(containsRelationshipColumn(relationships, extraChildColumnId,
                extraParentColumnId)).isTrue();
    }

    @Test
    @DisplayName("NON_IDENTIFYING 관계 컬럼 삭제 시 FK 컬럼만 제거된다")
    void removeRelationshipColumnDoesNotChangeConstraints() throws Exception {
        SetupState state = setupNonIdentifyingRelationship().state();

        Validation.Table parentTable = buildParentTableWithPk(state,
                List.of(buildColumn(state.parentColumnId(),
                        state.parentTableId(), "id", 1, true)));
        Validation.Table childTable = buildChildTableWithRelationship(state);

        JsonNode extraParentResponse = postJson("/columns",
                toJson(buildCreateColumnRequest(state.schemaId(),
                        state.parentTableId(),
                        REL_EXTRA_PARENT_COLUMN_REQUEST_ID, "code", 2, false,
                        List.of(parentTable, childTable))));
        String extraParentColumnId = extractNestedMapping(extraParentResponse,
                "columns", state.parentTableId(),
                REL_EXTRA_PARENT_COLUMN_REQUEST_ID);

        Validation.Table parentWithExtra = buildParentTableWithPk(state,
                List.of(buildColumn(state.parentColumnId(),
                        state.parentTableId(), "id", 1, true),
                        buildColumn(extraParentColumnId,
                                state.parentTableId(), "code", 2, false)));

        JsonNode extraChildResponse = postJson("/columns",
                toJson(buildCreateColumnRequest(state.schemaId(),
                        state.childTableId(),
                        REL_EXTRA_CHILD_COLUMN_REQUEST_ID, "parent_code", 3,
                        false,
                        List.of(parentWithExtra, childTable))));
        String extraChildColumnId = extractNestedMapping(extraChildResponse,
                "columns", state.childTableId(),
                REL_EXTRA_CHILD_COLUMN_REQUEST_ID);

        Validation.RelationshipColumn extraRelationshipColumn = buildRelationshipColumn(
                REL_EXTRA_REL_COLUMN_REQUEST_ID, state.relationshipId(),
                extraChildColumnId, extraParentColumnId, 2);
        Validation.Relationship existingRelationship = buildRelationship(state,
                List.of(buildRelationshipColumn(state.relationshipColumnId(),
                        state.relationshipId(), state.propagatedColumnId(),
                        state.parentColumnId(), 1)));

        Validation.Table childWithExtra = buildTable(state.childTableId(),
                state.schemaId(), "child",
                List.of(buildColumn(state.childColumnId(),
                        state.childTableId(), "id", 1, true),
                        buildColumn(state.propagatedColumnId(),
                                state.childTableId(), "parent_id", 2, false),
                        buildColumn(extraChildColumnId,
                                state.childTableId(), "parent_code", 3,
                                false)),
                List.of(), List.of(existingRelationship));

        Validation.AddColumnToRelationshipRequest addRequest = Validation.AddColumnToRelationshipRequest
                .newBuilder()
                .setDatabase(buildDatabase(state.schemaId(),
                        List.of(parentWithExtra, childWithExtra)))
                .setSchemaId(state.schemaId())
                .setRelationshipId(state.relationshipId())
                .setRelationshipColumn(extraRelationshipColumn)
                .build();

        JsonNode addResponse = postJson(
                "/relationships/" + state.relationshipId() + "/columns",
                toJson(addRequest));
        String mappedRelationshipColumnId = extractNestedMapping(addResponse,
                "relationshipColumns", state.relationshipId(),
                REL_EXTRA_REL_COLUMN_REQUEST_ID);

        Validation.Relationship updatedRelationship = buildRelationship(state,
                List.of(buildRelationshipColumn(state.relationshipColumnId(),
                        state.relationshipId(), state.propagatedColumnId(),
                        state.parentColumnId(), 1),
                        buildRelationshipColumn(mappedRelationshipColumnId,
                                state.relationshipId(), extraChildColumnId,
                                extraParentColumnId, 2)));

        Validation.Table childWithRelationshipColumns = buildTable(
                state.childTableId(),
                state.schemaId(),
                "child",
                List.of(buildColumn(state.childColumnId(),
                        state.childTableId(), "id", 1, true),
                        buildColumn(state.propagatedColumnId(),
                                state.childTableId(), "parent_id", 2, false),
                        buildColumn(extraChildColumnId,
                                state.childTableId(), "parent_code", 3,
                                false)),
                List.of(),
                List.of(updatedRelationship));

        Validation.RemoveColumnFromRelationshipRequest removeRequest = Validation.RemoveColumnFromRelationshipRequest
                .newBuilder()
                .setDatabase(buildDatabase(state.schemaId(),
                        List.of(parentWithExtra,
                                childWithRelationshipColumns)))
                .setSchemaId(state.schemaId())
                .setRelationshipId(state.relationshipId())
                .setRelationshipColumnId(mappedRelationshipColumnId)
                .build();

        JsonNode removeResponse = deleteJson(
                "/relationships/" + state.relationshipId() + "/columns/"
                        + mappedRelationshipColumnId,
                toJson(removeRequest));
        assertThat(removeResponse.path("success").asBoolean()).isTrue();

        JsonNode childTableResponse = getJson(
                "/tables/" + state.childTableId());
        JsonNode columns = childTableResponse.path("result")
                .path("columns");
        JsonNode constraints = childTableResponse.path("result")
                .path("constraints");
        JsonNode relationships = childTableResponse.path("result")
                .path("relationships");

        assertThat(containsId(columns, extraChildColumnId)).isFalse();
        assertThat(constraints).isEmpty();
        assertThat(relationshipColumnCount(relationships)).isEqualTo(1);
        assertThat(containsRelationshipColumn(relationships,
                state.propagatedColumnId(), state.parentColumnId())).isTrue();
    }

    @Test
    @DisplayName("IDENTIFYING -> NON_IDENTIFYING 전환 시 제약 전파가 제거된다")
    void changeRelationshipKindToNonIdentifying() throws Exception {
        SetupState state = setupIdentifyingRelationship().state();

        Validation.DeleteRelationshipRequest deleteRequest = Validation.DeleteRelationshipRequest
                .newBuilder()
                .setDatabase(buildDatabaseWithRelationship(state))
                .setSchemaId(state.schemaId())
                .setRelationshipId(state.relationshipId())
                .build();

        JsonNode deleteResponse = deleteJson(
                "/relationships/" + state.relationshipId(),
                toJson(deleteRequest));
        assertThat(deleteResponse.path("success").asBoolean()).isTrue();

        Validation.Table parentTable = buildParentTableWithPk(state,
                List.of(buildColumn(state.parentColumnId(),
                        state.parentTableId(), "id", 1, true)));
        Validation.Table childTable = buildTable(state.childTableId(),
                state.schemaId(), "child",
                List.of(buildColumn(state.childColumnId(),
                        state.childTableId(), "id", 1, true)),
                List.of(), List.of());

        JsonNode createResponse = postJson("/relationships",
                toJson(buildCreateRelationshipRequest(state.schemaId(),
                        state.parentTableId(), state.childTableId(),
                        state.parentColumnId(),
                        Validation.RelationshipKind.NON_IDENTIFYING,
                        List.of(parentTable, childTable))));

        JsonNode propagated = createResponse.path("result").path("propagated");
        assertThat(propagated.path("columns")).hasSize(1);
        assertThat(propagated.path("constraints")).isEmpty();
        assertThat(propagated.path("constraintColumns")).isEmpty();

        String newFkColumnId = propagated.path("columns").get(0)
                .path("columnId").asText();
        JsonNode childTableResponse = getJson(
                "/tables/" + state.childTableId());
        JsonNode columns = childTableResponse.path("result")
                .path("columns");
        JsonNode constraints = childTableResponse.path("result")
                .path("constraints");

        assertThat(containsId(columns, state.childColumnId())).isTrue();
        assertThat(containsId(columns, newFkColumnId)).isTrue();
        assertThat(constraints).isEmpty();
    }

    @Test
    @DisplayName("NON_IDENTIFYING -> IDENTIFYING 전환 시 제약 전파가 생성된다")
    void changeRelationshipKindToIdentifying() throws Exception {
        SetupState state = setupNonIdentifyingRelationship().state();

        Validation.DeleteRelationshipRequest deleteRequest = Validation.DeleteRelationshipRequest
                .newBuilder()
                .setDatabase(buildDatabaseWithRelationship(state))
                .setSchemaId(state.schemaId())
                .setRelationshipId(state.relationshipId())
                .build();

        JsonNode deleteResponse = deleteJson(
                "/relationships/" + state.relationshipId(),
                toJson(deleteRequest));
        assertThat(deleteResponse.path("success").asBoolean()).isTrue();

        Validation.Table parentTable = buildParentTableWithPk(state,
                List.of(buildColumn(state.parentColumnId(),
                        state.parentTableId(), "id", 1, true)));
        Validation.Table childTable = buildTable(state.childTableId(),
                state.schemaId(), "child",
                List.of(buildColumn(state.childColumnId(),
                        state.childTableId(), "id", 1, true)),
                List.of(), List.of());

        JsonNode createResponse = postJson("/relationships",
                toJson(buildCreateRelationshipRequest(state.schemaId(),
                        state.parentTableId(), state.childTableId(),
                        state.parentColumnId(),
                        Validation.RelationshipKind.IDENTIFYING,
                        List.of(parentTable, childTable))));

        JsonNode propagated = createResponse.path("result").path("propagated");
        assertThat(propagated.path("columns")).hasSize(1);
        assertThat(propagated.path("constraints")).hasSize(1);
        assertThat(propagated.path("constraintColumns")).hasSize(1);

        String newFkColumnId = propagated.path("columns").get(0)
                .path("columnId").asText();
        JsonNode childTableResponse = getJson(
                "/tables/" + state.childTableId());
        JsonNode columns = childTableResponse.path("result")
                .path("columns");
        JsonNode constraints = childTableResponse.path("result")
                .path("constraints");

        assertThat(containsId(columns, state.childColumnId())).isTrue();
        assertThat(containsId(columns, newFkColumnId)).isTrue();
        assertThat(constraints).isNotEmpty();
    }

    @Test
    @DisplayName("NON_IDENTIFYING 관계 삭제 시 FK 컬럼만 제거된다")
    void deleteNonIdentifyingRelationshipRemovesFkColumn() throws Exception {
        SetupState state = setupNonIdentifyingRelationship().state();

        Validation.Database database = buildDatabaseWithRelationship(state);
        Validation.DeleteRelationshipRequest deleteRequest = Validation.DeleteRelationshipRequest
                .newBuilder()
                .setDatabase(database)
                .setSchemaId(state.schemaId())
                .setRelationshipId(state.relationshipId())
                .build();

        JsonNode deleteResponse = deleteJson(
                "/relationships/" + state.relationshipId(),
                toJson(deleteRequest));
        assertThat(deleteResponse.path("success").asBoolean()).isTrue();

        JsonNode childTableResponse = getJson(
                "/tables/" + state.childTableId());
        JsonNode columns = childTableResponse.path("result")
                .path("columns");
        JsonNode constraints = childTableResponse.path("result")
                .path("constraints");
        JsonNode relationships = childTableResponse.path("result")
                .path("relationships");

        assertThat(containsId(columns, state.childColumnId())).isTrue();
        assertThat(containsId(columns, state.propagatedColumnId())).isFalse();
        assertThat(constraints).isEmpty();
        assertThat(relationships).isEmpty();
    }

    @Test
    @DisplayName("PK 컬럼 추가 시 FK/제약조건/관계 컬럼이 전파된다")
    void addPkColumnPropagatesToIdentifyingChild() throws Exception {
        SetupState state = setupIdentifyingRelationship().state();

        Validation.Table parentTable = buildParentTableWithPk(state,
                List.of(buildColumn(state.parentColumnId(),
                        state.parentTableId(), "id", 1, true)));
        Validation.Table childTable = buildChildTableWithRelationship(state);

        JsonNode extraColumnResponse = postJson("/columns",
                toJson(buildCreateColumnRequest(state.schemaId(),
                        state.parentTableId(),
                        EXTRA_PARENT_COLUMN_REQUEST_ID, "code", 2, false,
                        List.of(parentTable, childTable))));
        String extraParentColumnId = extractNestedMapping(extraColumnResponse,
                "columns", state.parentTableId(),
                EXTRA_PARENT_COLUMN_REQUEST_ID);

        Validation.Table parentWithExtraColumn = buildParentTableWithPk(state,
                List.of(buildColumn(state.parentColumnId(),
                        state.parentTableId(), "id", 1, true),
                        buildColumn(extraParentColumnId,
                                state.parentTableId(), "code", 2, false)));

        Validation.AddColumnToConstraintRequest addColumnRequest = Validation.AddColumnToConstraintRequest
                .newBuilder()
                .setDatabase(buildDatabase(state.schemaId(),
                        List.of(parentWithExtraColumn, childTable)))
                .setSchemaId(state.schemaId())
                .setTableId(state.parentTableId())
                .setConstraintId(state.parentPkId())
                .setConstraintColumn(Validation.ConstraintColumn.newBuilder()
                        .setId(EXTRA_PARENT_PK_COLUMN_REQUEST_ID)
                        .setColumnId(extraParentColumnId)
                        .setSeqNo(2)
                        .build())
                .build();

        JsonNode addResponse = postJson(
                "/constraints/" + state.parentPkId() + "/columns",
                toJson(addColumnRequest));

        String mappedConstraintColumnId = extractNestedMapping(addResponse,
                "constraintColumns", state.parentPkId(),
                EXTRA_PARENT_PK_COLUMN_REQUEST_ID);
        assertThat(mappedConstraintColumnId).isNotBlank()
                .isNotEqualTo(EXTRA_PARENT_PK_COLUMN_REQUEST_ID);

        JsonNode propagated = addResponse.path("result").path("propagated");
        assertThat(propagated.path("columns")).hasSize(1);
        assertThat(propagated.path("constraintColumns")).hasSize(1);
        assertThat(propagated.path("relationshipColumns")).hasSize(1);
        assertThat(propagated.path("constraints")).isEmpty();
        assertThat(propagated.path("indexColumns")).isEmpty();

        JsonNode propagatedColumn = propagated.path("columns").get(0);
        String propagatedColumnId = propagatedColumn.path("columnId").asText();
        assertThat(propagatedColumn.path("sourceType").asText())
                .isEqualTo(EntityType.CONSTRAINT.name());
        assertThat(propagatedColumn.path("sourceId").asText())
                .isEqualTo(state.parentPkId());
        assertThat(propagatedColumn.path("sourceColumnId").asText())
                .isEqualTo(extraParentColumnId);

        JsonNode propagatedConstraintColumn = propagated
                .path("constraintColumns").get(0);
        assertThat(propagatedConstraintColumn.path("constraintId").asText())
                .isEqualTo(state.propagatedConstraintId());
        assertThat(propagatedConstraintColumn.path("columnId").asText())
                .isEqualTo(propagatedColumnId);

        JsonNode propagatedRelationshipColumn = propagated
                .path("relationshipColumns").get(0);
        assertThat(propagatedRelationshipColumn.path("relationshipId").asText())
                .isEqualTo(state.relationshipId());
        assertThat(propagatedRelationshipColumn.path("fkColumnId").asText())
                .isEqualTo(propagatedColumnId);
        assertThat(propagatedRelationshipColumn.path("refColumnId").asText())
                .isEqualTo(extraParentColumnId);
    }

    @Test
    @DisplayName("자식 테이블 삭제 시 관계가 정리되고 부모는 유지된다")
    void deleteChildTableRemovesRelationshipAndColumns() throws Exception {
        SetupState state = setupIdentifyingRelationship().state();
        Validation.Database database = buildDatabaseWithRelationship(state);

        Validation.DeleteTableRequest deleteRequest = Validation.DeleteTableRequest
                .newBuilder()
                .setDatabase(database)
                .setSchemaId(state.schemaId())
                .setTableId(state.childTableId())
                .build();

        JsonNode deleteResponse = deleteJson(
                "/tables/" + state.childTableId(),
                toJson(deleteRequest));
        assertThat(deleteResponse.path("success").asBoolean()).isTrue();

        JsonNode childNotFoundResponse = getJsonExpect4xx(
                "/tables/" + state.childTableId());
        assertThat(childNotFoundResponse.path("success").asBoolean())
                .isFalse();

        JsonNode parentResponse = getJson(
                "/tables/" + state.parentTableId());
        assertThat(parentResponse.path("success").asBoolean()).isTrue();
        assertThat(containsId(parentResponse.path("result").path("columns"),
                state.parentColumnId())).isTrue();
    }

    @Test
    @DisplayName("테이블 삭제 시 관계 및 전파된 컬럼이 제거된다")
    void deleteTableRemovesRelationshipsAndColumns() throws Exception {
        SetupState state = setupIdentifyingRelationship().state();
        Validation.Database database = buildDatabaseWithRelationship(state);

        Validation.DeleteTableRequest deleteRequest = Validation.DeleteTableRequest
                .newBuilder()
                .setDatabase(database)
                .setSchemaId(state.schemaId())
                .setTableId(state.parentTableId())
                .build();

        JsonNode deleteResponse = deleteJson(
                "/tables/" + state.parentTableId(),
                toJson(deleteRequest));
        assertThat(deleteResponse.path("success").asBoolean()).isTrue();

        JsonNode childTableResponse = getJson(
                "/tables/" + state.childTableId());
        JsonNode columns = childTableResponse.path("result")
                .path("columns");
        JsonNode constraints = childTableResponse.path("result")
                .path("constraints");
        JsonNode relationships = childTableResponse.path("result")
                .path("relationships");

        assertThat(containsId(columns, state.childColumnId())).isTrue();
        assertThat(containsId(columns, state.propagatedColumnId())).isFalse();
        assertThat(containsId(constraints, state.propagatedConstraintId()))
                .isFalse();
        assertThat(relationships).isEmpty();

        JsonNode parentNotFoundResponse = getJsonExpect4xx(
                "/tables/" + state.parentTableId());
        assertThat(parentNotFoundResponse.path("success").asBoolean())
                .isFalse();
    }

    @Test
    @DisplayName("PK 컬럼 삭제 시 전파된 FK 컬럼도 제거되어야 한다 (Cascade 지원 확인)")
    void deletePkColumnRemovesPropagatedFkColumn() throws Exception {
        SetupState state = setupIdentifyingRelationship().state();

        Validation.Database database = buildDatabaseWithRelationship(state);
        Validation.DeleteColumnRequest deleteRequest = Validation.DeleteColumnRequest
                .newBuilder()
                .setDatabase(database)
                .setSchemaId(state.schemaId())
                .setTableId(state.parentTableId())
                .setColumnId(state.parentColumnId())
                .build();

        JsonNode deleteResponse = deleteJson(
                "/columns/" + state.parentColumnId(),
                toJson(deleteRequest));
        assertThat(deleteResponse.path("success").asBoolean()).isTrue();

        JsonNode childTableResponse = getJson(
                "/tables/" + state.childTableId());
        JsonNode columns = childTableResponse.path("result")
                .path("columns");

        assertThat(containsId(columns, state.propagatedColumnId())).isFalse();
    }

    private JsonNode postJson(String path, String body) throws Exception {
        String responseBody = webTestClient.post()
                .uri(API_BASE_PATH + path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return objectMapper.readTree(responseBody);
    }

    private JsonNode deleteJson(String path, String body) throws Exception {
        String responseBody = webTestClient.method(HttpMethod.DELETE)
                .uri(API_BASE_PATH + path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return objectMapper.readTree(responseBody);
    }

    private JsonNode getJson(String path) throws Exception {
        String responseBody = webTestClient.get()
                .uri(API_BASE_PATH + path)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return objectMapper.readTree(responseBody);
    }

    private JsonNode getJsonExpect4xx(String path) throws Exception {
        String responseBody = webTestClient.get()
                .uri(API_BASE_PATH + path)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return objectMapper.readTree(responseBody);
    }

    private boolean containsId(JsonNode arrayNode, String id) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return false;
        }
        for (JsonNode node : arrayNode) {
            if (id.equals(node.path("id").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRelationshipColumn(JsonNode relationships,
            String fkColumnId, String refColumnId) {
        if (relationships == null || !relationships.isArray()) {
            return false;
        }
        for (JsonNode relationship : relationships) {
            JsonNode columns = relationship.path("columns");
            if (!columns.isArray()) {
                continue;
            }
            for (JsonNode column : columns) {
                String fk = column.path("srcColumnId").asText();
                if (fk.isEmpty()) {
                    fk = column.path("fkColumnId").asText();
                }
                String ref = column.path("tgtColumnId").asText();
                if (ref.isEmpty()) {
                    ref = column.path("refColumnId").asText();
                }
                if (fkColumnId.equals(fk) && refColumnId.equals(ref)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int relationshipColumnCount(JsonNode relationships) {
        if (relationships == null || !relationships.isArray()) {
            return 0;
        }
        int count = 0;
        for (JsonNode relationship : relationships) {
            JsonNode columns = relationship.path("columns");
            if (columns.isArray()) {
                count += columns.size();
            }
        }
        return count;
    }

    private String extractMapping(JsonNode response, String field,
            String requestId) {
        String value = response.path("result").path(field).path(requestId)
                .asText();
        assertThat(value).as("mapping for %s", requestId).isNotBlank();
        return value;
    }

    private String extractNestedMapping(JsonNode response, String field,
            String key, String requestId) {
        String value = response.path("result").path(field).path(key)
                .path(requestId).asText();
        assertThat(value).as("mapping for %s/%s", key, requestId).isNotBlank();
        return value;
    }

    private String toJson(Message message) throws Exception {
        return JsonFormat.printer().print(message);
    }

    private SetupResult setupIdentifyingRelationship() throws Exception {
        return setupRelationship(Validation.RelationshipKind.IDENTIFYING);
    }

    private SetupResult setupNonIdentifyingRelationship() throws Exception {
        return setupRelationship(Validation.RelationshipKind.NON_IDENTIFYING);
    }

    private SetupResult setupRelationship(Validation.RelationshipKind kind)
            throws Exception {
        JsonNode schemaResponse = postJson("/schemas",
                toJson(buildCreateSchemaRequest()));
        String schemaId = extractMapping(schemaResponse, "schemas",
                SCHEMA_REQUEST_ID);

        JsonNode parentTableResponse = postJson("/tables",
                toJson(buildCreateTableRequest(schemaId,
                        PARENT_TABLE_REQUEST_ID, "parent", List.of())));
        String parentTableId = extractMapping(parentTableResponse, "tables",
                PARENT_TABLE_REQUEST_ID);

        Validation.Table parentTableState = buildTable(parentTableId, schemaId,
                "parent", List.of(), List.of(), List.of());
        JsonNode childTableResponse = postJson("/tables",
                toJson(buildCreateTableRequest(schemaId,
                        CHILD_TABLE_REQUEST_ID, "child",
                        List.of(parentTableState))));
        String childTableId = extractMapping(childTableResponse, "tables",
                CHILD_TABLE_REQUEST_ID);

        Validation.Table childTableState = buildTable(childTableId, schemaId,
                "child", List.of(), List.of(), List.of());
        JsonNode parentColumnResponse = postJson("/columns",
                toJson(buildCreateColumnRequest(schemaId, parentTableId,
                        PARENT_COLUMN_REQUEST_ID, "id", 1, true,
                        List.of(parentTableState, childTableState))));
        String parentColumnId = extractNestedMapping(parentColumnResponse,
                "columns", parentTableId, PARENT_COLUMN_REQUEST_ID);

        Validation.Table parentWithColumn = buildTable(parentTableId, schemaId,
                "parent",
                List.of(buildColumn(parentColumnId, parentTableId, "id", 1,
                        true)),
                List.of(), List.of());
        JsonNode childColumnResponse = postJson("/columns",
                toJson(buildCreateColumnRequest(schemaId, childTableId,
                        CHILD_COLUMN_REQUEST_ID, "id", 1, true,
                        List.of(parentWithColumn, childTableState))));
        String childColumnId = extractNestedMapping(childColumnResponse,
                "columns", childTableId, CHILD_COLUMN_REQUEST_ID);

        Validation.Table childWithColumn = buildTable(childTableId, schemaId,
                "child",
                List.of(buildColumn(childColumnId, childTableId, "id", 1,
                        true)),
                List.of(), List.of());
        JsonNode constraintResponse = postJson("/constraints",
                toJson(buildCreatePkRequest(schemaId, parentTableId,
                        parentColumnId,
                        List.of(parentWithColumn, childWithColumn))));
        String parentPkId = extractNestedMapping(constraintResponse,
                "constraints", parentTableId, PK_REQUEST_ID);
        String parentPkColumnId = extractNestedMapping(constraintResponse,
                "constraintColumns", parentPkId, PK_COLUMN_REQUEST_ID);

        Validation.Table parentWithPk = buildTable(parentTableId, schemaId,
                "parent",
                List.of(buildColumn(parentColumnId, parentTableId, "id", 1,
                        true)),
                List.of(buildPkConstraint(parentPkId, parentTableId,
                        parentPkColumnId, parentColumnId)),
                List.of());

        JsonNode relationshipResponse = postJson("/relationships",
                toJson(buildCreateRelationshipRequest(schemaId, parentTableId,
                        childTableId, parentColumnId, kind,
                        List.of(parentWithPk, childWithColumn))));

        String relationshipId = extractNestedMapping(relationshipResponse,
                "relationships", childTableId, RELATIONSHIP_REQUEST_ID);
        String relationshipColumnId = extractNestedMapping(relationshipResponse,
                "relationshipColumns", relationshipId, REL_COLUMN_REQUEST_ID);

        JsonNode propagated = relationshipResponse.path("result")
                .path("propagated");
        JsonNode propagatedColumn = propagated.path("columns").get(0);
        JsonNode constraints = propagated.path("constraints");
        JsonNode constraintColumns = propagated.path("constraintColumns");

        String propagatedConstraintId = "";
        if (constraints.isArray() && constraints.size() > 0) {
            propagatedConstraintId = constraints.get(0).path("constraintId")
                    .asText();
        }

        String propagatedConstraintColumnId = "";
        if (constraintColumns.isArray() && constraintColumns.size() > 0) {
            propagatedConstraintColumnId = constraintColumns.get(0)
                    .path("constraintColumnId")
                    .asText();
        }

        SetupState state = new SetupState(
                schemaId,
                parentTableId,
                childTableId,
                parentColumnId,
                childColumnId,
                parentPkId,
                parentPkColumnId,
                relationshipId,
                relationshipColumnId,
                kind,
                propagatedColumn.path("columnId").asText(),
                propagatedConstraintId,
                propagatedConstraintColumnId,
                propagatedColumn.path("sourceType").asText(),
                propagatedColumn.path("sourceId").asText(),
                propagatedColumn.path("sourceColumnId").asText());

        return new SetupResult(state, relationshipResponse);
    }

    private Validation.CreateSchemaRequest buildCreateSchemaRequest() {
        Validation.Schema schema = Validation.Schema.newBuilder()
                .setId(SCHEMA_REQUEST_ID)
                .setProjectId(DATABASE_ID)
                .setDbVendorId(Validation.DbVendor.MYSQL)
                .setName("main")
                .setCharset("utf8mb4")
                .setCollation("utf8mb4_general_ci")
                .setVendorOption("")
                .build();

        return Validation.CreateSchemaRequest.newBuilder()
                .setDatabase(
                        Validation.Database.newBuilder().setId(DATABASE_ID))
                .setSchema(schema)
                .build();
    }

    private Validation.CreateTableRequest buildCreateTableRequest(
            String schemaId,
            String tableId,
            String name,
            List<Validation.Table> existingTables) {
        Validation.Table table = buildTable(tableId, schemaId, name, List.of(),
                List.of(), List.of());
        Validation.Database database = buildDatabase(schemaId, existingTables);
        return Validation.CreateTableRequest.newBuilder()
                .setDatabase(database)
                .setSchemaId(schemaId)
                .setTable(table)
                .build();
    }

    private Validation.CreateColumnRequest buildCreateColumnRequest(
            String schemaId,
            String tableId,
            String columnId,
            String name,
            int ordinalPosition,
            boolean isAutoIncrement,
            List<Validation.Table> tables) {
        Validation.Database database = buildDatabase(schemaId, tables);
        Validation.Column column = buildColumn(columnId, tableId, name,
                ordinalPosition, isAutoIncrement);
        return Validation.CreateColumnRequest.newBuilder()
                .setDatabase(database)
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setColumn(column)
                .build();
    }

    private Validation.CreateConstraintRequest buildCreatePkRequest(
            String schemaId,
            String tableId,
            String columnId,
            List<Validation.Table> tables) {
        Validation.ConstraintColumn constraintColumn = Validation.ConstraintColumn
                .newBuilder()
                .setId(PK_COLUMN_REQUEST_ID)
                .setConstraintId(PK_REQUEST_ID)
                .setColumnId(columnId)
                .setSeqNo(1)
                .build();

        Validation.Constraint constraint = Validation.Constraint.newBuilder()
                .setId(PK_REQUEST_ID)
                .setTableId(tableId)
                .setName("PK")
                .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                .addColumns(constraintColumn)
                .build();

        Validation.Database database = buildDatabase(schemaId, tables);
        return Validation.CreateConstraintRequest.newBuilder()
                .setDatabase(database)
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setConstraint(constraint)
                .build();
    }

    private Validation.CreateRelationshipRequest buildCreateRelationshipRequest(
            String schemaId,
            String parentTableId,
            String childTableId,
            String parentColumnId,
            Validation.RelationshipKind kind,
            List<Validation.Table> tables) {
        Validation.RelationshipColumn relationshipColumn = Validation.RelationshipColumn
                .newBuilder()
                .setId(REL_COLUMN_REQUEST_ID)
                .setRelationshipId(RELATIONSHIP_REQUEST_ID)
                .setFkColumnId(FK_COLUMN_REQUEST_ID)
                .setRefColumnId(parentColumnId)
                .setSeqNo(1)
                .build();

        Validation.Relationship relationship = Validation.Relationship
                .newBuilder()
                .setId(RELATIONSHIP_REQUEST_ID)
                .setSrcTableId(childTableId)
                .setTgtTableId(parentTableId)
                .setName("FK_test")
                .setKind(kind)
                .setCardinality(
                        Validation.RelationshipCardinality.ONE_TO_MANY)
                .setOnDelete(Validation.RelationshipOnDelete.NO_ACTION)
                .setOnUpdate(Validation.RelationshipOnUpdate.NO_ACTION_UPDATE)
                .setFkEnforced(false)
                .addColumns(relationshipColumn)
                .build();

        Validation.Database database = buildDatabase(schemaId, tables);
        return Validation.CreateRelationshipRequest.newBuilder()
                .setDatabase(database)
                .setSchemaId(schemaId)
                .setRelationship(relationship)
                .build();
    }

    private Validation.Database buildDatabase(String schemaId,
            List<Validation.Table> tables) {
        Validation.Schema schema = Validation.Schema.newBuilder()
                .setId(schemaId)
                .setProjectId(DATABASE_ID)
                .setDbVendorId(Validation.DbVendor.MYSQL)
                .setName("main")
                .setCharset("utf8mb4")
                .setCollation("utf8mb4_general_ci")
                .setVendorOption("")
                .addAllTables(tables)
                .build();

        return Validation.Database.newBuilder()
                .setId(DATABASE_ID)
                .addSchemas(schema)
                .build();
    }

    private Validation.Table buildTable(String tableId, String schemaId,
            String name, List<Validation.Column> columns,
            List<Validation.Constraint> constraints,
            List<Validation.Relationship> relationships) {
        return Validation.Table.newBuilder()
                .setId(tableId)
                .setSchemaId(schemaId)
                .setName(name)
                .setComment("")
                .setTableOptions("")
                .addAllColumns(columns)
                .addAllConstraints(constraints)
                .addAllRelationships(relationships)
                .build();
    }

    private Validation.Column buildColumn(String columnId, String tableId,
            String name, int ordinalPosition, boolean isAutoIncrement) {
        return Validation.Column.newBuilder()
                .setId(columnId)
                .setTableId(tableId)
                .setName(name)
                .setOrdinalPosition(ordinalPosition)
                .setDataType("INT")
                .setIsAutoIncrement(isAutoIncrement)
                .build();
    }

    private Validation.Constraint buildPkConstraint(String constraintId,
            String tableId, String constraintColumnId, String columnId) {
        return buildPkConstraint(constraintId, tableId, constraintColumnId,
                columnId, "PK");
    }

    private Validation.Constraint buildPkConstraint(String constraintId,
            String tableId, String constraintColumnId, String columnId,
            String name) {
        Validation.ConstraintColumn constraintColumn = Validation.ConstraintColumn
                .newBuilder()
                .setId(constraintColumnId)
                .setConstraintId(constraintId)
                .setColumnId(columnId)
                .setSeqNo(1)
                .build();

        return Validation.Constraint.newBuilder()
                .setId(constraintId)
                .setTableId(tableId)
                .setName(name)
                .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                .addColumns(constraintColumn)
                .build();
    }

    private Validation.Relationship buildRelationship(SetupState state,
            String fkColumnId) {
        return buildRelationship(state, List.of(buildRelationshipColumn(
                state.relationshipColumnId(), state.relationshipId(),
                fkColumnId,
                state.parentColumnId(), 1)));
    }

    private Validation.Relationship buildRelationship(SetupState state,
            List<Validation.RelationshipColumn> columns) {
        return Validation.Relationship.newBuilder()
                .setId(state.relationshipId())
                .setSrcTableId(state.childTableId())
                .setTgtTableId(state.parentTableId())
                .setName("FK_test")
                .setKind(state.relationshipKind())
                .setCardinality(
                        Validation.RelationshipCardinality.ONE_TO_MANY)
                .setOnDelete(Validation.RelationshipOnDelete.NO_ACTION)
                .setOnUpdate(Validation.RelationshipOnUpdate.NO_ACTION_UPDATE)
                .setFkEnforced(false)
                .addAllColumns(columns)
                .build();
    }

    private Validation.RelationshipColumn buildRelationshipColumn(String id,
            String relationshipId, String fkColumnId, String refColumnId,
            int seqNo) {
        return Validation.RelationshipColumn.newBuilder()
                .setId(id)
                .setRelationshipId(relationshipId)
                .setFkColumnId(fkColumnId)
                .setRefColumnId(refColumnId)
                .setSeqNo(seqNo)
                .build();
    }

    private Validation.Table buildParentTableWithPk(SetupState state,
            List<Validation.Column> columns) {
        Validation.Constraint parentPk = buildPkConstraint(state.parentPkId(),
                state.parentTableId(), state.parentPkColumnId(),
                state.parentColumnId());
        return buildTable(state.parentTableId(), state.schemaId(), "parent",
                columns, List.of(parentPk), List.of());
    }

    private Validation.Table buildChildTableWithRelationship(SetupState state) {
        Validation.Column childIdColumn = buildColumn(state.childColumnId(),
                state.childTableId(), "id", 1, true);
        Validation.Column fkColumn = buildColumn(state.propagatedColumnId(),
                state.childTableId(), "parent_id", 2, false);
        Validation.Relationship relationship = buildRelationship(state,
                state.propagatedColumnId());
        List<Validation.Constraint> constraints = List.of();
        if (state
                .relationshipKind() == Validation.RelationshipKind.IDENTIFYING) {
            Validation.Constraint childPk = buildPkConstraint(
                    state.propagatedConstraintId(), state.childTableId(),
                    state.propagatedConstraintColumnId(),
                    state.propagatedColumnId(), "pk_child");
            constraints = List.of(childPk);
        }

        return buildTable(state.childTableId(), state.schemaId(), "child",
                List.of(childIdColumn, fkColumn),
                constraints, List.of(relationship));
    }

    private Validation.Database buildDatabaseWithRelationship(
            SetupState state) {
        Validation.Table parentTable = buildParentTableWithPk(state,
                List.of(buildColumn(state.parentColumnId(),
                        state.parentTableId(), "id", 1, true)));
        Validation.Table childTable = buildChildTableWithRelationship(state);
        return buildDatabase(state.schemaId(),
                List.of(parentTable, childTable));
    }

    private record SetupState(
            String schemaId,
            String parentTableId,
            String childTableId,
            String parentColumnId,
            String childColumnId,
            String parentPkId,
            String parentPkColumnId,
            String relationshipId,
            String relationshipColumnId,
            Validation.RelationshipKind relationshipKind,
            String propagatedColumnId,
            String propagatedConstraintId,
            String propagatedConstraintColumnId,
            String propagatedSourceType,
            String propagatedSourceId,
            String propagatedSourceColumnId) {
    }

    private record SetupResult(SetupState state,
            JsonNode relationshipResponse) {
    }

}
