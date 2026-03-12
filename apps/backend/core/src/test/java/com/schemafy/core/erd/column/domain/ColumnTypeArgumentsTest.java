package com.schemafy.core.erd.column.domain;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ColumnTypeArguments")
class ColumnTypeArgumentsTest {

  @Nested
  @DisplayName("생성 시")
  class WhenCreating {

    @Test
    @DisplayName("length만으로 생성된다")
    void createsWithLengthOnly() {
      var typeArguments = new ColumnTypeArguments(255, null, null);

      assertThat(typeArguments.length()).isEqualTo(255);
      assertThat(typeArguments.precision()).isNull();
      assertThat(typeArguments.scale()).isNull();
      assertThat(typeArguments.hasLength()).isTrue();
      assertThat(typeArguments.hasPrecisionScale()).isFalse();
    }

    @Test
    @DisplayName("precision과 scale로 생성된다")
    void createsWithPrecisionAndScale() {
      var typeArguments = new ColumnTypeArguments(null, 10, 2);

      assertThat(typeArguments.length()).isNull();
      assertThat(typeArguments.precision()).isEqualTo(10);
      assertThat(typeArguments.scale()).isEqualTo(2);
      assertThat(typeArguments.hasLength()).isFalse();
      assertThat(typeArguments.hasPrecisionScale()).isTrue();
    }

    @Test
    @DisplayName("values로 생성된다")
    void createsWithValuesOnly() {
      var typeArguments = new ColumnTypeArguments(null, null, null, List.of("A", "B"));

      assertThat(typeArguments.length()).isNull();
      assertThat(typeArguments.precision()).isNull();
      assertThat(typeArguments.scale()).isNull();
      assertThat(typeArguments.values()).containsExactly("A", "B");
      assertThat(typeArguments.hasValues()).isTrue();
    }

    @Test
    @DisplayName("length와 precision/scale을 동시에 설정하면 예외가 발생한다")
    void throwsWhenLengthAndPrecisionBothSet() {
      assertThatThrownBy(() -> new ColumnTypeArguments(255, 10, 2))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("length cannot be combined with precision/scale");
    }

    @Test
    @DisplayName("values와 length/precision/scale을 동시에 설정하면 예외가 발생한다")
    void throwsWhenValuesCombinedWithLengthOrPrecisionScale() {
      assertThatThrownBy(() -> new ColumnTypeArguments(255, null, null, List.of("A")))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("length cannot be combined");

      assertThatThrownBy(() -> new ColumnTypeArguments(null, 10, 2, List.of("A")))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("values cannot be combined");
    }

    @Test
    @DisplayName("length가 0 이하이면 예외가 발생한다")
    void throwsWhenLengthIsZeroOrNegative() {
      assertThatThrownBy(() -> new ColumnTypeArguments(0, null, null))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("length must be positive");

      assertThatThrownBy(() -> new ColumnTypeArguments(-1, null, null))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("length must be positive");
    }

    @Test
    @DisplayName("precision이 0 이하이면 예외가 발생한다")
    void throwsWhenPrecisionIsZeroOrNegative() {
      assertThatThrownBy(() -> new ColumnTypeArguments(null, 0, 2))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("precision must be positive");

      assertThatThrownBy(() -> new ColumnTypeArguments(null, -1, 2))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("precision must be positive");
    }

    @Test
    @DisplayName("scale이 음수이면 예외가 발생한다")
    void throwsWhenScaleIsNegative() {
      assertThatThrownBy(() -> new ColumnTypeArguments(null, 10, -1))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("scale must be zero or positive");
    }

    @Test
    @DisplayName("precision 없이 scale만 설정하면 예외가 발생한다")
    void throwsWhenScaleWithoutPrecision() {
      assertThatThrownBy(() -> new ColumnTypeArguments(null, null, 2))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("precision is required when scale is provided");
    }

    @Test
    @DisplayName("scale 없이 precision만 설정하면 예외가 발생한다")
    void throwsWhenPrecisionWithoutScale() {
      assertThatThrownBy(() -> new ColumnTypeArguments(null, 10, null))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("scale is required when precision is provided");
    }

    @Test
    @DisplayName("values가 비어있으면 예외가 발생한다")
    void throwsWhenValuesEmpty() {
      assertThatThrownBy(() -> new ColumnTypeArguments(null, null, null, List.of()))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("values must not be empty");
    }

    @Test
    @DisplayName("values에 blank가 포함되면 예외가 발생한다")
    void throwsWhenValuesContainBlank() {
      assertThatThrownBy(() -> new ColumnTypeArguments(null, null, null, List.of("A", "  ")))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("trim 후 values가 중복이면 예외가 발생한다")
    void throwsWhenValuesDuplicatedAfterTrim() {
      assertThatThrownBy(() -> new ColumnTypeArguments(null, null, null, List.of("A", " A ")))
          .isInstanceOf(DomainException.class)
          .hasMessageContaining("duplicates");
    }

  }

  @Nested
  @DisplayName("from 팩토리 메서드는")
  class FromMethod {

    @Test
    @DisplayName("모든 인자가 null이면 null을 반환한다")
    void returnsNullWhenAllNull() {
      var result = ColumnTypeArguments.from(null, null, null);

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("length가 있으면 ColumnTypeArguments을 반환한다")
    void returnsTypeArgumentsWhenLengthProvided() {
      var result = ColumnTypeArguments.from(255, null, null);

      assertThat(result).isNotNull();
      assertThat(result.length()).isEqualTo(255);
    }

    @Test
    @DisplayName("precision/scale이 있으면 ColumnTypeArguments을 반환한다")
    void returnsTypeArgumentsWhenPrecisionScaleProvided() {
      var result = ColumnTypeArguments.from(null, 10, 2);

      assertThat(result).isNotNull();
      assertThat(result.precision()).isEqualTo(10);
      assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("values가 있으면 ColumnTypeArguments을 반환한다")
    void returnsTypeArgumentsWhenValuesProvided() {
      var result = ColumnTypeArguments.from(null, null, null, List.of("A", "B"));

      assertThat(result).isNotNull();
      assertThat(result.values()).containsExactly("A", "B");
    }

  }

  @Nested
  @DisplayName("toJson 메서드는")
  class ToJsonMethod {

    @Test
    @DisplayName("length가 있으면 length JSON을 반환한다")
    void returnsLengthJson() {
      var typeArguments = new ColumnTypeArguments(255, null, null);

      assertThat(typeArguments.toJson()).isEqualTo("{\"length\":255}");
    }

    @Test
    @DisplayName("precision/scale이 있으면 precision/scale JSON을 반환한다")
    void returnsPrecisionScaleJson() {
      var typeArguments = new ColumnTypeArguments(null, 10, 2);

      assertThat(typeArguments.toJson()).isEqualTo("{\"precision\":10,\"scale\":2}");
    }

    @Test
    @DisplayName("values가 있으면 values JSON을 반환한다")
    void returnsValuesJson() {
      var typeArguments = new ColumnTypeArguments(null, null, null, List.of("A", "B"));

      assertThat(typeArguments.toJson()).isEqualTo("{\"values\":[\"A\",\"B\"]}");
    }

  }

  @Nested
  @DisplayName("fromJson 메서드는")
  class FromJsonMethod {

    @Test
    @DisplayName("null이면 null을 반환한다")
    void returnsNullWhenJsonIsNull() {
      assertThat(ColumnTypeArguments.fromJson(null)).isNull();
    }

    @Test
    @DisplayName("빈 문자열이면 null을 반환한다")
    void returnsNullWhenJsonIsBlank() {
      assertThat(ColumnTypeArguments.fromJson("")).isNull();
      assertThat(ColumnTypeArguments.fromJson("   ")).isNull();
    }

    @Test
    @DisplayName("length JSON을 파싱한다")
    void parsesLengthJson() {
      var result = ColumnTypeArguments.fromJson("{\"length\":255}");

      assertThat(result).isNotNull();
      assertThat(result.length()).isEqualTo(255);
      assertThat(result.precision()).isNull();
      assertThat(result.scale()).isNull();
    }

    @Test
    @DisplayName("precision/scale JSON을 파싱한다")
    void parsesPrecisionScaleJson() {
      var result = ColumnTypeArguments.fromJson("{\"precision\":10,\"scale\":2}");

      assertThat(result).isNotNull();
      assertThat(result.length()).isNull();
      assertThat(result.precision()).isEqualTo(10);
      assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("values JSON을 파싱한다")
    void parsesValuesJson() {
      var result = ColumnTypeArguments.fromJson("{\"values\":[\"A\",\"B\"]}");

      assertThat(result).isNotNull();
      assertThat(result.length()).isNull();
      assertThat(result.precision()).isNull();
      assertThat(result.scale()).isNull();
      assertThat(result.values()).containsExactly("A", "B");
    }

    @Test
    @DisplayName("유효하지 않은 values JSON이면 null을 반환한다")
    void returnsNullWhenInvalidValuesJson() {
      assertThat(ColumnTypeArguments.fromJson("{\"values\":[A,B]}")).isNull();
      assertThat(ColumnTypeArguments.fromJson("{\"values\":[\"A\",]}")).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 JSON이면 null을 반환한다")
    void returnsNullWhenInvalidJson() {
      assertThat(ColumnTypeArguments.fromJson("invalid")).isNull();
      assertThat(ColumnTypeArguments.fromJson("{}")).isNull();
    }

  }

  @Nested
  @DisplayName("isEmpty 메서드는")
  class IsEmptyMethod {

    @Test
    @DisplayName("length가 있으면 false를 반환한다")
    void returnsFalseWhenHasLength() {
      var typeArguments = new ColumnTypeArguments(255, null, null);

      assertThat(typeArguments.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("precision/scale이 있으면 false를 반환한다")
    void returnsFalseWhenHasPrecisionScale() {
      var typeArguments = new ColumnTypeArguments(null, 10, 2);

      assertThat(typeArguments.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("values가 있으면 false를 반환한다")
    void returnsFalseWhenHasValues() {
      var typeArguments = new ColumnTypeArguments(null, null, null, List.of("A", "B"));

      assertThat(typeArguments.isEmpty()).isFalse();
    }

  }

}
