package com.schemafy.domain.ulid.application.service;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.ulid.exception.UlidErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UlidGeneratorTest {

  @Test
  void generateUlid() {
    String ulid = UlidGenerator.generate();

    assertThat(ulid).isNotNull();
    assertThat(ulid).hasSize(26);
    assertThat(ulid).matches("[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}");
  }

  @RepeatedTest(10)
  void generateUniqueUlids() {
    String ulid1 = UlidGenerator.generate();
    String ulid2 = UlidGenerator.generate();

    assertThat(ulid1).isNotEqualTo(ulid2);
  }

  @Test
  void generateSortableUlids() throws InterruptedException {
    String ulid1 = UlidGenerator.generate();

    Thread.sleep(1);

    String ulid2 = UlidGenerator.generate();

    assertThat(ulid1).isLessThan(ulid2);
  }

  @Test
  void extractTimestamp() {
    String ulid = UlidGenerator.generate();
    long timestamp = UlidGenerator.extractTimestamp(ulid);

    assertThat(timestamp).isPositive();
    assertThat(Math.abs(System.currentTimeMillis() - timestamp))
        .isLessThan(60_000L);
  }

  @Test
  void extractTimestampWithInvalidUlid() {
    assertThatThrownBy(() -> UlidGenerator.extractTimestamp("invalid"))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(UlidErrorCode.INVALID_VALUE);

    assertThatThrownBy(() -> UlidGenerator.extractTimestamp(null))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(UlidErrorCode.INVALID_VALUE);

    assertThatThrownBy(() -> UlidGenerator.extractTimestamp("short"))
        .isInstanceOf(DomainException.class)
        .extracting(error -> ((DomainException) error).getErrorCode())
        .isEqualTo(UlidErrorCode.INVALID_VALUE);
  }

}
