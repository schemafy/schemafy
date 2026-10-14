package com.schemafy.core.erd.vendor.domain;

public record IdentifierCapabilities(
    int maxLength,
    IdentifierLengthUnit lengthUnit) {

  public IdentifierCapabilities {
    if (maxLength < 1) {
      throw new IllegalArgumentException("maxLength must be positive");
    }
    if (lengthUnit == null) {
      throw new IllegalArgumentException("lengthUnit must not be null");
    }
  }

  public static IdentifierCapabilities codePoints(int maxLength) {
    return new IdentifierCapabilities(maxLength, IdentifierLengthUnit.CODE_POINTS);
  }

  public boolean allows(String value) {
    return value == null || lengthUnit.measure(value) <= maxLength;
  }

  public boolean allows(String value, int localMaxCodePoints) {
    validateLocalMaxCodePoints(localMaxCodePoints);
    return value == null
        || allows(value) && codePointLength(value) <= localMaxCodePoints;
  }

  public String fitGeneratedName(String baseName, String suffix) {
    validateGeneratedNameParts(baseName, suffix);
    return fitToVendorLimit(baseName, suffix);
  }

  public String fitGeneratedName(
      String baseName,
      String suffix,
      int localMaxCodePoints) {
    validateGeneratedNameParts(baseName, suffix);
    validateLocalMaxCodePoints(localMaxCodePoints);
    int localSuffixLength = codePointLength(suffix);
    if (localSuffixLength >= localMaxCodePoints) {
      throw new IllegalArgumentException("suffix must be shorter than the local identifier limit");
    }
    String locallyFittedBase = IdentifierLengthUnit.CODE_POINTS.take(
        baseName,
        localMaxCodePoints - localSuffixLength);
    return fitToVendorLimit(locallyFittedBase, suffix);
  }

  private String fitToVendorLimit(String baseName, String suffix) {
    int suffixLength = lengthUnit.measure(suffix);
    if (suffixLength >= maxLength) {
      throw new IllegalArgumentException("suffix must be shorter than the vendor identifier limit");
    }
    return lengthUnit.take(baseName, maxLength - suffixLength) + suffix;
  }

  private static void validateGeneratedNameParts(String baseName, String suffix) {
    if (baseName == null) {
      throw new IllegalArgumentException("baseName must not be null");
    }
    if (suffix == null) {
      throw new IllegalArgumentException("suffix must not be null");
    }
  }

  private static void validateLocalMaxCodePoints(int localMaxCodePoints) {
    if (localMaxCodePoints < 1) {
      throw new IllegalArgumentException("localMaxCodePoints must be positive");
    }
  }

  private static int codePointLength(String value) {
    return value.codePointCount(0, value.length());
  }

}
