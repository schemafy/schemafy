package com.schemafy.core.erd.vendor.domain.datatype;

import java.util.Set;

import com.schemafy.core.erd.index.domain.type.IndexType;

public record DatatypeProperties(
    boolean autoIncrementAllowed,
    boolean charsetCollationAllowed,
    Set<IndexType> indexTypes,
    String foreignKeyGroup) {

  public DatatypeProperties {
    if (indexTypes == null) {
      throw new IllegalArgumentException("Datatype indexTypes must not be null");
    }
    indexTypes = Set.copyOf(indexTypes);
    if (foreignKeyGroup != null) {
      foreignKeyGroup = foreignKeyGroup.trim();
      if (foreignKeyGroup.isEmpty()) {
        foreignKeyGroup = null;
      }
    }
  }

}
