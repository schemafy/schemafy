import type { ErdStore } from '@/store/erd.store';
import type {
  PropagatedEntitiesGroup,
  SyncContext,
} from '@/features/drawing/types';
import type { IdMappingWithType } from '../../index';
import { findSchema } from '../../helpers';
import {
  replaceConstraintIds,
  replacePropagatedColumns,
  replacePropagatedRelationshipColumns,
} from './shared';

export function relationshipStrategy(
  _relationshipId: string,
  entities: PropagatedEntitiesGroup,
  context: SyncContext,
  erdStore: ErdStore,
  idMap: Map<string, IdMappingWithType>,
) {
  const schema = findSchema(erdStore, context.schemaId);

  if (entities.relationshipColumns.length > 0) {
    replacePropagatedRelationshipColumns(
      entities.relationshipColumns,
      schema,
      context,
      erdStore,
      idMap,
    );
  } else if (entities.columns.length > 0) {
    replacePropagatedColumns(
      entities.columns,
      schema,
      context,
      erdStore,
      idMap,
    );
  }

  replaceConstraintIds(entities, context, erdStore, idMap);
}
