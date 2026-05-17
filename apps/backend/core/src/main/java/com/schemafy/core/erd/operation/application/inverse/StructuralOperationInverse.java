package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public sealed interface StructuralOperationInverse permits
    CreateTableInverse,
    DeleteTableInverse,
    CreateColumnInverse,
    DeleteColumnInverse,
    CreateConstraintInverse,
    DeleteConstraintInverse,
    AddConstraintColumnInverse,
    RemoveConstraintColumnInverse,
    CreateIndexInverse,
    DeleteIndexInverse,
    AddIndexColumnInverse,
    RemoveIndexColumnInverse,
    CreateRelationshipInverse,
    ChangeRelationshipKindInverse,
    DeleteRelationshipInverse,
    AddRelationshipColumnInverse,
    RemoveRelationshipColumnInverse {

  String schemaId();

  String touchedEntityId();

  StructuralSnapshot beforeSnapshot();

  StructuralSnapshot afterSnapshot();

  List<String> affectedTableIds();

}
