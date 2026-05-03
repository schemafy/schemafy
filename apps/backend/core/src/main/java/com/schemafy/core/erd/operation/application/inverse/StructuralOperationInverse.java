package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public sealed interface StructuralOperationInverse permits
    AddConstraintColumnInverse,
    RemoveConstraintColumnInverse,
    AddIndexColumnInverse,
    RemoveIndexColumnInverse,
    AddRelationshipColumnInverse,
    RemoveRelationshipColumnInverse {

  String schemaId();

  String touchedEntityId();

  StructuralSnapshot beforeSnapshot();

  StructuralSnapshot afterSnapshot();

  List<String> affectedTableIds();

}
