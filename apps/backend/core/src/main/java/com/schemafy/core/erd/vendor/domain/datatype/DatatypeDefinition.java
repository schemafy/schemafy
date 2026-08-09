package com.schemafy.core.erd.vendor.domain.datatype;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.schemafy.core.erd.column.domain.ColumnTypeArguments;

public record DatatypeDefinition(
    String sqlType,
    List<String> aliases,
    String displayName,
    String category,
    List<DatatypeParameter> parameters,
    String sqlDeclarationTemplate,
    DatatypeProperties properties) {

  public DatatypeDefinition {
    sqlType = normalizeSqlToken(sqlType, "sqlType");
    aliases = normalizeAliases(aliases);
    displayName = requireText(displayName, "displayName");
    category = requireText(category, "category");
    parameters = normalizeParameters(parameters);
    if (properties == null) {
      throw new IllegalArgumentException("Datatype properties must not be null");
    }
    if (sqlDeclarationTemplate == null || sqlDeclarationTemplate.isBlank()) {
      if (!parameters.isEmpty()) {
        throw new IllegalArgumentException("Parameterized datatype requires a declaration template");
      }
      sqlDeclarationTemplate = sqlType;
    } else {
      sqlDeclarationTemplate = sqlDeclarationTemplate.trim();
    }
    validateTemplate(sqlType, parameters, sqlDeclarationTemplate);
  }

  public String render(
      ColumnTypeArguments arguments,
      UnaryOperator<String> escapeString) {
    if (escapeString == null) {
      throw new IllegalArgumentException("escapeString must not be null");
    }
    Map<DatatypeParameterName, Object> values = argumentValues(arguments);
    int[] cursor = { 0 };
    Rendered rendered = renderSegment(sqlDeclarationTemplate, cursor, values, escapeString, false);
    if (!rendered.complete()) {
      throw new IllegalArgumentException("Required datatype declaration argument is missing");
    }
    return rendered.text();
  }

  Map<DatatypeParameterName, DatatypeParameter> parametersByName() {
    return parameters.stream().collect(Collectors.toUnmodifiableMap(
        DatatypeParameter::name,
        parameter -> parameter));
  }

  static Map<DatatypeParameterName, Object> argumentValues(ColumnTypeArguments arguments) {
    Map<DatatypeParameterName, Object> values = new HashMap<>();
    if (arguments == null) {
      return values;
    }
    values.put(DatatypeParameterName.LENGTH, arguments.length());
    values.put(DatatypeParameterName.PRECISION, arguments.precision());
    values.put(DatatypeParameterName.SCALE, arguments.scale());
    values.put(DatatypeParameterName.VALUES, arguments.hasValues() ? arguments.values() : null);
    return values;
  }

  private static Rendered renderSegment(
      String template,
      int[] cursor,
      Map<DatatypeParameterName, Object> values,
      UnaryOperator<String> escapeString,
      boolean optional) {
    StringBuilder rendered = new StringBuilder();
    boolean complete = true;
    while (cursor[0] < template.length()) {
      char current = template.charAt(cursor[0]);
      if (current == ']') {
        cursor[0]++;
        break;
      }
      if (current == '[') {
        cursor[0]++;
        Rendered nested = renderSegment(template, cursor, values, escapeString, true);
        if (nested.complete()) {
          rendered.append(nested.text());
        }
        continue;
      }
      if (current == '{') {
        int end = template.indexOf('}', cursor[0] + 1);
        String placeholder = template.substring(cursor[0] + 1, end);
        DatatypeParameterName name = DatatypeParameterName.fromJson(placeholder);
        Object value = values.get(name);
        if (value == null) {
          complete = false;
        } else {
          rendered.append(renderValue(value, escapeString));
        }
        cursor[0] = end + 1;
        continue;
      }
      rendered.append(current);
      cursor[0]++;
    }
    return optional && !complete
        ? new Rendered("", false)
        : new Rendered(rendered.toString(), complete);
  }

  private static String renderValue(
      Object value,
      UnaryOperator<String> escapeString) {
    if (value instanceof Integer integer) {
      return integer.toString();
    }
    if (value instanceof List<?> values) {
      return values.stream()
          .map(String.class::cast)
          .map(item -> "'" + escapeString.apply(item) + "'")
          .collect(Collectors.joining(", "));
    }
    throw new IllegalArgumentException("Unsupported datatype declaration value");
  }

  private static List<String> normalizeAliases(List<String> aliases) {
    if (aliases == null) {
      return List.of();
    }
    List<String> normalized = aliases.stream()
        .map(alias -> normalizeSqlToken(alias, "alias"))
        .toList();
    if (new HashSet<>(normalized).size() != normalized.size()) {
      throw new IllegalArgumentException("Datatype aliases contain a collision");
    }
    return List.copyOf(normalized);
  }

  private static List<DatatypeParameter> normalizeParameters(
      List<DatatypeParameter> parameters) {
    if (parameters == null) {
      return List.of();
    }
    List<DatatypeParameter> sorted = new ArrayList<>(parameters);
    if (sorted.stream().anyMatch(parameter -> parameter == null)) {
      throw new IllegalArgumentException("Datatype parameters must not contain null");
    }
    sorted.sort(Comparator.comparingInt(DatatypeParameter::order));
    Set<DatatypeParameterName> names = new HashSet<>();
    for (int index = 0; index < sorted.size(); index++) {
      DatatypeParameter parameter = sorted.get(index);
      if (parameter.order() != index + 1) {
        throw new IllegalArgumentException("Datatype parameter order must be contiguous");
      }
      if (!names.add(parameter.name())) {
        throw new IllegalArgumentException("Duplicate datatype parameter: " + parameter.name().jsonName());
      }
    }
    int precisionIndex = indexOf(sorted, DatatypeParameterName.PRECISION);
    int scaleIndex = indexOf(sorted, DatatypeParameterName.SCALE);
    if (scaleIndex >= 0 && (precisionIndex < 0 || precisionIndex > scaleIndex)) {
      throw new IllegalArgumentException("Scale parameter requires preceding precision parameter");
    }
    return List.copyOf(sorted);
  }

  private static int indexOf(
      List<DatatypeParameter> parameters,
      DatatypeParameterName name) {
    for (int index = 0; index < parameters.size(); index++) {
      if (parameters.get(index).name() == name) {
        return index;
      }
    }
    return -1;
  }

  private static void validateTemplate(
      String sqlType,
      List<DatatypeParameter> parameters,
      String template) {
    if (!template.startsWith(sqlType)) {
      throw new IllegalArgumentException("Datatype declaration template must start with canonical sqlType");
    }
    Map<String, DatatypeParameter> parameterByName = parameters.stream()
        .collect(Collectors.toMap(
            parameter -> parameter.name().jsonName(),
            parameter -> parameter));
    Set<String> placeholders = new HashSet<>();
    int optionalDepth = 0;
    for (int index = sqlType.length(); index < template.length(); index++) {
      char current = template.charAt(index);
      if (current == '[') {
        optionalDepth++;
        if (optionalDepth > 2) {
          throw new IllegalArgumentException("Datatype template optional nesting exceeds two levels");
        }
      } else if (current == ']') {
        optionalDepth--;
        if (optionalDepth < 0) {
          throw new IllegalArgumentException("Datatype template has unbalanced optional segments");
        }
      } else if (current == '{') {
        int end = template.indexOf('}', index + 1);
        if (end < 0) {
          throw new IllegalArgumentException("Datatype template has an unbalanced placeholder");
        }
        String name = template.substring(index + 1, end);
        DatatypeParameter parameter = parameterByName.get(name);
        if (parameter == null || !placeholders.add(name)) {
          throw new IllegalArgumentException("Datatype template placeholder is unknown or duplicated: " + name);
        }
        if (parameter.required() && optionalDepth > 0) {
          throw new IllegalArgumentException("Required datatype placeholder cannot be optional: " + name);
        }
        if (!parameter.required() && optionalDepth == 0) {
          throw new IllegalArgumentException("Optional datatype placeholder must be in an optional segment: " + name);
        }
        index = end;
      } else if (current == '}') {
        throw new IllegalArgumentException("Datatype template has an unbalanced placeholder");
      } else if ("(), ".indexOf(current) < 0) {
        throw new IllegalArgumentException("Datatype template contains an unsupported literal");
      }
    }
    if (optionalDepth != 0) {
      throw new IllegalArgumentException("Datatype template has unbalanced optional segments");
    }
    if (!placeholders.equals(parameterByName.keySet())) {
      throw new IllegalArgumentException("Datatype template placeholders do not match parameters");
    }
  }

  private static String normalizeSqlToken(String value, String name) {
    String normalized = requireText(value, name).toUpperCase(Locale.ROOT);
    if (!normalized.matches("[A-Z][A-Z0-9_]*")) {
      throw new IllegalArgumentException("Datatype " + name + " has an invalid SQL token");
    }
    return normalized;
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Datatype " + name + " must not be blank");
    }
    return value.trim();
  }

  private record Rendered(String text, boolean complete) {
  }

}
