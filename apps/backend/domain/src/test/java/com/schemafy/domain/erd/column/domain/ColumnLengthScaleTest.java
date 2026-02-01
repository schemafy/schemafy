package com.schemafy.domain.erd.column.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.exception.InvalidValueException;

@DisplayName("ColumnLengthScale")
class ColumnLengthScaleTest {

  @Nested
  @DisplayName("생성 시")
  class WhenCreating {

    @Test
    @DisplayName("length만으로 생성된다")
    void createsWithLengthOnly() {
      var lengthScale = new ColumnLengthScale(255, null, null);

      assertThat(lengthScale.length()).isEqualTo(255);
      assertThat(lengthScale.precision()).isNull();
      assertThat(lengthScale.scale()).isNull();
      assertThat(lengthScale.hasLength()).isTrue();
      assertThat(lengthScale.hasPrecisionScale()).isFalse();
    }

    @Test
    @DisplayName("precision과 scale로 생성된다")
    void createsWithPrecisionAndScale() {
      var lengthScale = new ColumnLengthScale(null, 10, 2);

      assertThat(lengthScale.length()).isNull();
      assertThat(lengthScale.precision()).isEqualTo(10);
      assertThat(lengthScale.scale()).isEqualTo(2);
      assertThat(lengthScale.hasLength()).isFalse();
      assertThat(lengthScale.hasPrecisionScale()).isTrue();
    }

    @Test
    @DisplayName("length와 precision/scale을 동시에 설정하면 예외가 발생한다")
    void throwsWhenLengthAndPrecisionBothSet() {
      assertThatThrownBy(() -> new ColumnLengthScale(255, 10, 2))
          .isInstanceOf(InvalidValueException.class)
          .hasMessageContaining("length cannot be combined with precision/scale");
    }

    @Test
    @DisplayName("length가 0 이하이면 예외가 발생한다")
    void throwsWhenLengthIsZeroOrNegative() {
      assertThatThrownBy(() -> new ColumnLengthScale(0, null, null))
          .isInstanceOf(InvalidValueException.class)
          .hasMessageContaining("length must be positive");

      assertThatThrownBy(() -> new ColumnLengthScale(-1, null, null))
          .isInstanceOf(InvalidValueException.class)
          .hasMessageContaining("length must be positive");
    }

    @Test
    @DisplayName("precision이 0 이하이면 예외가 발생한다")
    void throwsWhenPrecisionIsZeroOrNegative() {
      assertThatThrownBy(() -> new ColumnLengthScale(null, 0, 2))
          .isInstanceOf(InvalidValueException.class)
          .hasMessageContaining("precision must be positive");

      assertThatThrownBy(() -> new ColumnLengthScale(null, -1, 2))
          .isInstanceOf(InvalidValueException.class)
          .hasMessageContaining("precision must be positive");
    }

    @Test
    @DisplayName("scale이 음수이면 예외가 발생한다")
    void throwsWhenScaleIsNegative() {
      assertThatThrownBy(() -> new ColumnLengthScale(null, 10, -1))
          .isInstanceOf(InvalidValueException.class)
          .hasMessageContaining("scale must be zero or positive");
    }

    @Test
    @DisplayName("precision 없이 scale만 설정하면 예외가 발생한다")
    void throwsWhenScaleWithoutPrecision() {
      assertThatThrownBy(() -> new ColumnLengthScale(null, null, 2))
          .isInstanceOf(InvalidValueException.class)
          .hasMessageContaining("precision is required when scale is provided");
    }

    @Test
    @DisplayName("scale 없이 precision만 설정하면 예외가 발생한다")
    void throwsWhenPrecisionWithoutScale() {
      assertThatThrownBy(() -> new ColumnLengthScale(null, 10, null))
          .isInstanceOf(InvalidValueException.class)
          .hasMessageContaining("scale is required when precision is provided");
    }

  }

  @Nested
  @DisplayName("from 팩토리 메서드는")
  class FromMethod {

    @Test
    @DisplayName("모든 인자가 null이면 null을 반환한다")
    void returnsNullWhenAllNull() {
      var result = ColumnLengthScale.from(null, null, null);

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("length가 있으면 ColumnLengthScale을 반환한다")
    void returnsLengthScaleWhenLengthProvided() {
      var result = ColumnLengthScale.from(255, null, null);

      assertThat(result).isNotNull();
      assertThat(result.length()).isEqualTo(255);
    }

    @Test
    @DisplayName("precision/scale이 있으면 ColumnLengthScale을 반환한다")
    void returnsLengthScaleWhenPrecisionScaleProvided() {
      var result = ColumnLengthScale.from(null, 10, 2);

      assertThat(result).isNotNull();
      assertThat(result.precision()).isEqualTo(10);
      assertThat(result.scale()).isEqualTo(2);
    }

  }

  @Nested
  @DisplayName("toJson 메서드는")
  class ToJsonMethod {

    @Test
    @DisplayName("length가 있으면 length JSON을 반환한다")
    void returnsLengthJson() {
      var lengthScale = new ColumnLengthScale(255, null, null);

      assertThat(lengthScale.toJson()).isEqualTo("{\"length\":255}");
    }

    @Test
    @DisplayName("precision/scale이 있으면 precision/scale JSON을 반환한다")
    void returnsPrecisionScaleJson() {
      var lengthScale = new ColumnLengthScale(null, 10, 2);

      assertThat(lengthScale.toJson()).isEqualTo("{\"precision\":10,\"scale\":2}");
    }

  }

  @Nested
  @DisplayName("fromJson 메서드는")
  class FromJsonMethod {

    @Test
    @DisplayName("null이면 null을 반환한다")
    void returnsNullWhenJsonIsNull() {
      assertThat(ColumnLengthScale.fromJson(null)).isNull();
    }

    @Test
    @DisplayName("빈 문자열이면 null을 반환한다")
    void returnsNullWhenJsonIsBlank() {
      assertThat(ColumnLengthScale.fromJson("")).isNull();
      assertThat(ColumnLengthScale.fromJson("   ")).isNull();
    }

    @Test
    @DisplayName("length JSON을 파싱한다")
    void parsesLengthJson() {
      var result = ColumnLengthScale.fromJson("{\"length\":255}");

      assertThat(result).isNotNull();
      assertThat(result.length()).isEqualTo(255);
      assertThat(result.precision()).isNull();
      assertThat(result.scale()).isNull();
    }

    @Test
    @DisplayName("precision/scale JSON을 파싱한다")
    void parsesPrecisionScaleJson() {
      var result = ColumnLengthScale.fromJson("{\"precision\":10,\"scale\":2}");

      assertThat(result).isNotNull();
      assertThat(result.length()).isNull();
      assertThat(result.precision()).isEqualTo(10);
      assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("유효하지 않은 JSON이면 null을 반환한다")
    void returnsNullWhenInvalidJson() {
      assertThat(ColumnLengthScale.fromJson("invalid")).isNull();
      assertThat(ColumnLengthScale.fromJson("{}")).isNull();
    }

  }

  @Nested
  @DisplayName("isEmpty 메서드는")
  class IsEmptyMethod {

    @Test
    @DisplayName("length가 있으면 false를 반환한다")
    void returnsFalseWhenHasLength() {
      var lengthScale = new ColumnLengthScale(255, null, null);

      assertThat(lengthScale.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("precision/scale이 있으면 false를 반환한다")
    void returnsFalseWhenHasPrecisionScale() {
      var lengthScale = new ColumnLengthScale(null, 10, 2);

      assertThat(lengthScale.isEmpty()).isFalse();
    }

  }

}
