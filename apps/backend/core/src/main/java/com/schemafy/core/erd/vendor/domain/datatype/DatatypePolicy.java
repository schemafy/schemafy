package com.schemafy.core.erd.vendor.domain.datatype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.schemafy.core.common.exception.DomainErrorCode;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;

public record DatatypePolicy(
    int schemaVersion,
    String vendor,
    String version,
    String versionRange,
    List<DatatypeDefinition> types) {

  private static final int SUPPORTED_SCHEMA_VERSION = 2;
  private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");
  private static final Pattern RANGE_TERM_PATTERN = Pattern.compile(
      "\\s*(>=|<=|>|<|=)\\s*(\\d+(?:\\.\\d+)*)");

  public DatatypePolicy {
    if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
      throw new IllegalArgumentException("Unsupported datatype policy schemaVersion: " + schemaVersion);
    }
    if (vendor == null || vendor.isBlank()) {
      throw new IllegalArgumentException("Datatype policy vendor must not be blank");
    }
    vendor = vendor.trim().toLowerCase(Locale.ROOT);
    version = normalizeOptional(version);
    versionRange = normalizeOptional(versionRange);
    if ((version == null) == (versionRange == null)) {
      throw new IllegalArgumentException(
          "Datatype policy requires exactly one of version or versionRange");
    }
    if (version != null) {
      parseVersion(version);
    } else {
      parseRange(versionRange);
    }
    if (types == null || types.isEmpty()) {
      throw new IllegalArgumentException("Datatype policy types must not be empty");
    }
    if (types.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Datatype policy types must not contain null");
    }
    validateTypeIdentityCollisions(types);
    types = List.copyOf(types);
  }

  public boolean matchesVersion(String candidate) {
    Version parsedCandidate = parseVersion(candidate);
    if (version != null) {
      return parsedCandidate.compareTo(parseVersion(version)) == 0;
    }
    return parseRange(versionRange).stream()
        .allMatch(term -> term.matches(parsedCandidate));
  }

  public void validateIdentity(String candidateVendor, String candidateVersion) {
    String normalizedVendor = candidateVendor == null
        ? null
        : candidateVendor.trim().toLowerCase(Locale.ROOT);
    if (!vendor.equals(normalizedVendor)) {
      throw new IllegalArgumentException(
          "Datatype policy vendor does not match DB vendor: " + candidateVendor);
    }
    if (!matchesVersion(candidateVersion)) {
      throw new IllegalArgumentException(
          "Datatype policy version does not match DB vendor: " + candidateVersion);
    }
  }

  public Optional<DatatypeDefinition> find(String dataType) {
    if (dataType == null || dataType.isBlank()) {
      return Optional.empty();
    }
    String normalized = dataType.trim().toUpperCase(Locale.ROOT);
    return types.stream()
        .filter(type -> type.sqlType().equals(normalized)
            || type.aliases().contains(normalized))
        .findFirst();
  }

  public DatatypeDefinition validate(
      String dataType,
      ColumnTypeArguments arguments,
      boolean autoIncrement,
      String charset,
      String collation,
      DatatypeValidationErrorCodes errorCodes) {
    Objects.requireNonNull(errorCodes, "errorCodes");
    DatatypeDefinition definition = find(dataType)
        .orElseThrow(() -> new DomainException(
            errorCodes.unsupportedType(),
            dataType == null || dataType.isBlank()
                ? "Column data type must not be blank"
                : "Unsupported column data type: " + dataType));
    Map<DatatypeParameterName, DatatypeParameter> parameters = definition.parametersByName();
    Map<DatatypeParameterName, Object> values = DatatypeDefinition.argumentValues(arguments);

    for (DatatypeParameterName name : DatatypeParameterName.values()) {
      DatatypeParameter parameter = parameters.get(name);
      Object value = values.get(name);
      if (parameter == null && value != null) {
        throw invalidArgument(errorCodes, definition, name, "is not allowed");
      }
      if (parameter != null && parameter.required() && value == null) {
        throw new DomainException(requiredErrorCode(errorCodes, name),
            "Required datatype parameter is missing: " + name.jsonName());
      }
      if (parameter != null && value != null) {
        validateParameterValue(errorCodes, definition, parameter, value);
      }
    }

    if (arguments != null
        && arguments.precision() != null
        && arguments.scale() != null
        && arguments.scale() > arguments.precision()) {
      throw invalidArgument(errorCodes, definition, DatatypeParameterName.SCALE,
          "must not exceed precision");
    }
    if (autoIncrement && !definition.properties().autoIncrementAllowed()) {
      throw new DomainException(errorCodes.autoIncrementNotAllowed(),
          "AUTO_INCREMENT is not allowed for data type: " + definition.sqlType());
    }
    if ((hasText(charset) || hasText(collation))
        && !definition.properties().charsetCollationAllowed()) {
      throw new DomainException(errorCodes.charsetNotAllowed(),
          "Charset or collation is not allowed for data type: " + definition.sqlType());
    }
    return definition;
  }

  private static void validateParameterValue(
      DatatypeValidationErrorCodes errorCodes,
      DatatypeDefinition definition,
      DatatypeParameter parameter,
      Object value) {
    if (parameter.valueType() == DatatypeParameterValueType.INTEGER) {
      int integer = (Integer) value;
      if (integer < parameter.minValue() || integer > parameter.maxValue()) {
        throw invalidArgument(errorCodes, definition, parameter.name(),
            "must be between %d and %d".formatted(parameter.minValue(), parameter.maxValue()));
      }
      return;
    }

    @SuppressWarnings("unchecked")
    List<String> values = (List<String>) value;
    if (values.size() < parameter.minItems() || values.size() > parameter.maxItems()) {
      throw invalidArgument(errorCodes, definition, parameter.name(),
          "must contain between %d and %d items".formatted(
              parameter.minItems(), parameter.maxItems()));
    }
    for (String item : values) {
      int length = item.codePointCount(0, item.length());
      if (length < parameter.minItemLength() || length > parameter.maxItemLength()) {
        throw invalidArgument(errorCodes, definition, parameter.name(),
            "item length must be between %d and %d".formatted(
                parameter.minItemLength(), parameter.maxItemLength()));
      }
    }
  }

  private static DomainErrorCode requiredErrorCode(
      DatatypeValidationErrorCodes errorCodes,
      DatatypeParameterName name) {
    return switch (name) {
    case LENGTH -> errorCodes.requiredLength();
    case PRECISION, SCALE -> errorCodes.requiredPrecision();
    case VALUES -> errorCodes.invalidArgument();
    };
  }

  private static DomainException invalidArgument(
      DatatypeValidationErrorCodes errorCodes,
      DatatypeDefinition definition,
      DatatypeParameterName name,
      String detail) {
    return new DomainException(errorCodes.invalidArgument(),
        "%s parameter %s %s".formatted(definition.sqlType(), name.jsonName(), detail));
  }

  private static void validateTypeIdentityCollisions(
      List<DatatypeDefinition> types) {
    Map<String, String> ownerByIdentity = new HashMap<>();
    for (DatatypeDefinition type : types) {
      registerIdentity(ownerByIdentity, type.sqlType(), type.sqlType());
      for (String alias : type.aliases()) {
        registerIdentity(ownerByIdentity, alias, type.sqlType());
      }
    }
  }

  private static void registerIdentity(
      Map<String, String> ownerByIdentity,
      String identity,
      String owner) {
    String previous = ownerByIdentity.putIfAbsent(identity, owner);
    if (previous != null) {
      throw new IllegalArgumentException(
          "Datatype canonical or alias collision: %s (%s, %s)"
              .formatted(identity, previous, owner));
    }
  }

  private static List<RangeTerm> parseRange(String expression) {
    Matcher matcher = RANGE_TERM_PATTERN.matcher(expression);
    List<RangeTerm> terms = new ArrayList<>();
    int cursor = 0;
    while (matcher.find()) {
      if (!expression.substring(cursor, matcher.start()).isBlank()) {
        throw new IllegalArgumentException("Malformed datatype policy versionRange: " + expression);
      }
      terms.add(new RangeTerm(matcher.group(1), parseVersion(matcher.group(2))));
      cursor = matcher.end();
    }
    if (terms.isEmpty() || !expression.substring(cursor).isBlank()) {
      throw new IllegalArgumentException("Malformed datatype policy versionRange: " + expression);
    }
    return List.copyOf(terms);
  }

  private static Version parseVersion(String value) {
    if (value == null || !VERSION_PATTERN.matcher(value.trim()).matches()) {
      throw new IllegalArgumentException("Malformed datatype policy version: " + value);
    }
    List<Integer> parts = List.of(value.trim().split("\\."))
        .stream()
        .map(Integer::parseInt)
        .toList();
    return new Version(parts);
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private record RangeTerm(String operator, Version version) {

    boolean matches(Version candidate) {
      int comparison = candidate.compareTo(version);
      return switch (operator) {
      case ">" -> comparison > 0;
      case ">=" -> comparison >= 0;
      case "<" -> comparison < 0;
      case "<=" -> comparison <= 0;
      case "=" -> comparison == 0;
      default -> throw new IllegalStateException("Unsupported version operator: " + operator);
      };
    }

  }

  private record Version(List<Integer> parts) implements Comparable<Version> {

    @Override
    public int compareTo(Version other) {
      int size = Math.max(parts.size(), other.parts.size());
      for (int index = 0; index < size; index++) {
        int left = index < parts.size() ? parts.get(index) : 0;
        int right = index < other.parts.size() ? other.parts.get(index) : 0;
        int comparison = Integer.compare(left, right);
        if (comparison != 0) {
          return comparison;
        }
      }
      return 0;
    }

  }

}
