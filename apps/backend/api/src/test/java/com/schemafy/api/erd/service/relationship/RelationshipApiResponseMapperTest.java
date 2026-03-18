package com.schemafy.api.erd.service.relationship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RelationshipApiResponseMapper")
class RelationshipApiResponseMapperTest {

  private final RelationshipApiResponseMapper sut = new RelationshipApiResponseMapper(
      new JsonCodec(new ObjectMapper().findAndRegisterModules()));

  @Test
  @DisplayName("relationship extra 문자열을 JSON 객체로 변환한다")
  void toRelationshipResponse_mapsExtraJson() {
    Relationship relationship = new Relationship(
        "rel-1",
        "pk-1",
        "fk-1",
        "fk_users_orders",
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        "{\"fkHandle\":\"left\"}");

    var response = sut.toRelationshipResponse(relationship);

    assertThat(response.extra()).isNotNull();
    assertThat(response.extra().get("fkHandle").textValue())
        .isEqualTo("left");
  }

  @Test
  @DisplayName("relationship extra가 null이면 그대로 null을 유지한다")
  void toRelationshipResponse_preservesNullExtra() {
    Relationship relationship = new Relationship(
        "rel-1",
        "pk-1",
        "fk-1",
        "fk_users_orders",
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null);

    var response = sut.toRelationshipResponse(relationship);

    assertThat(response.extra()).isNull();
  }

}
