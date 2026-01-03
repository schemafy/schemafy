import type { ErdStore } from '@/store/erd.store';
import type {
  PropagatedEntitiesGroup,
  SyncContext,
} from '@/features/drawing/types';
import { findSchema } from '../../helpers';
import {
  replaceConstraintIds,
  replacePropagatedColumns,
  replacePropagatedRelationshipColumns,
} from './shared';

export function constraintStrategy(
  _constraintId: string,
  entities: PropagatedEntitiesGroup,
  context: SyncContext,
  erdStore: ErdStore,
) {
  const schema = findSchema(erdStore, context.schemaId);

  if (entities.columns.length > 0) {
    replacePropagatedColumns(entities.columns, schema, context, erdStore);
  }

  if (entities.relationshipColumns.length > 0) {
    replacePropagatedRelationshipColumns(
      entities.relationshipColumns,
      schema,
      context,
      erdStore,
    );
  }

  replaceConstraintIds(entities, context, erdStore);
}
