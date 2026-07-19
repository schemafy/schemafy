package com.schemafy.core.erd.index.domain.policy;

import java.util.Set;

import com.schemafy.core.erd.index.domain.type.IndexType;

public record IndexCapabilities(
    Set<IndexType> supportedTypes,
    Set<IndexType> sortDirectionTypes) {

  public IndexCapabilities {
    if (supportedTypes == null) {
      throw new IllegalArgumentException("supportedTypes must not be null");
    }
    if (sortDirectionTypes == null) {
      throw new IllegalArgumentException("sortDirectionTypes must not be null");
    }
    supportedTypes = Set.copyOf(supportedTypes);
    sortDirectionTypes = Set.copyOf(sortDirectionTypes);
    if (!supportedTypes.containsAll(sortDirectionTypes)) {
      throw new IllegalArgumentException(
          "sortDirectionTypes must be included in supportedTypes");
    }
  }

  public boolean supports(IndexType type) {
    return type != null && supportedTypes.contains(type);
  }

  public boolean sortDirectionAffectsSemantics(IndexType type) {
    return type != null && sortDirectionTypes.contains(type);
  }

}
