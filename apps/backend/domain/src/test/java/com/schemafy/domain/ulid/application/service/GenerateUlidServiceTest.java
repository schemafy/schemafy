package com.schemafy.domain.ulid.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateUlidService")
class GenerateUlidServiceTest {

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @InjectMocks
  GenerateUlidService sut;

  @Test
  @DisplayName("generateUlid: ULID를 생성한다")
  void generateUlid_success() {
    given(ulidGeneratorPort.generate()).willReturn("01ARZ3NDEKTSV4RRFFQ69G5FAV");

    StepVerifier.create(sut.generateUlid())
        .assertNext(ulid -> assertThat(ulid).isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAV"))
        .verifyComplete();
  }

}
