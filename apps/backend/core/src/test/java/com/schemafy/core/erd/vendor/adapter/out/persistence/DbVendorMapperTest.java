package com.schemafy.core.erd.vendor.adapter.out.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DbVendorMapper")
class DbVendorMapperTest {

  @Mock
  DbVendorEntity entity;

  JsonCodec jsonCodec;
  DbVendorMapper sut;

  @BeforeEach
  void setUp() {
    jsonCodec = new JsonCodec(new ObjectMapper().findAndRegisterModules());
    sut = new DbVendorMapper(jsonCodec);
    given(entity.getId()).willReturn(1);
    given(entity.getName()).willReturn("mysql");
    given(entity.getVersion()).willReturn("8.0");
  }

  @Test
  @DisplayName("schema version 2 JSON을 typed datatype policy로 복원한다")
  void mapsValidTypedDatatypePolicy() {
    DatatypePolicy policy = DbVendorFixture.defaultDatatypePolicy();
    given(entity.getDisplayName()).willReturn("MySQL 8.0");
    given(entity.getDatatypeMappings()).willReturn(jsonCodec.toJson(policy));
    given(entity.getCapabilities()).willReturn(
        jsonCodec.toJson(DbVendorFixture.defaultCapabilities()));

    var vendor = sut.toDomain(entity);

    assertThat(vendor.datatypeMappings()).isEqualTo(policy);
    assertThat(vendor.datatypeMappings().find("INTEGER").orElseThrow().sqlType())
        .isEqualTo("INT");
  }

  @Test
  @DisplayName("지원하지 않는 policy schema version이면 vendor load를 거부한다")
  void rejectsUnsupportedPolicySchemaVersion() {
    given(entity.getDatatypeMappings()).willReturn("""
        {"schemaVersion":1,"vendor":"mysql","versionRange":">= 8.0 < 9.0","types":[]}
        """);

    assertThatThrownBy(() -> sut.toDomain(entity))
        .isInstanceOfSatisfying(DomainException.class,
            error -> assertThat(error.getErrorCode())
                .isEqualTo(VendorErrorCode.INVALID_DATATYPE_POLICY))
        .hasMessageContaining("mysql")
        .hasMessageContaining("8.0");
  }

  @Test
  @DisplayName("policy vendor가 entity vendor와 다르면 vendor load를 거부한다")
  void rejectsPolicyVendorMismatch() {
    String json = jsonCodec.toJson(DbVendorFixture.defaultDatatypePolicy())
        .replace("\"vendor\":\"mysql\"", "\"vendor\":\"postgresql\"");
    given(entity.getDatatypeMappings()).willReturn(json);

    assertThatThrownBy(() -> sut.toDomain(entity))
        .isInstanceOfSatisfying(DomainException.class,
            error -> assertThat(error.getErrorCode())
                .isEqualTo(VendorErrorCode.INVALID_DATATYPE_POLICY));
  }

  @Test
  @DisplayName("policy version 범위가 entity version을 포함하지 않으면 vendor load를 거부한다")
  void rejectsPolicyVersionMismatch() {
    String json = jsonCodec.toJson(DbVendorFixture.defaultDatatypePolicy())
        .replace(">= 8.0 < 9.0", ">= 9.0 < 10.0");
    given(entity.getDatatypeMappings()).willReturn(json);

    assertThatThrownBy(() -> sut.toDomain(entity))
        .isInstanceOfSatisfying(DomainException.class,
            error -> assertThat(error.getErrorCode())
                .isEqualTo(VendorErrorCode.INVALID_DATATYPE_POLICY));
  }

}
