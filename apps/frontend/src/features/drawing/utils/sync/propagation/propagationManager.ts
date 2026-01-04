import type { ErdStore } from '@/store/erd.store';
import type {
  PropagatedEntitiesGroup,
  SyncContext,
  SourceType,
} from '@/features/drawing/types';
import type { AffectedMappingResponse } from '@/features/drawing/api/types/common';
import type { IdMappingWithType } from '../index';
import { relationshipStrategy } from './strategies/relationshipStrategy';
import { constraintStrategy } from './strategies/constraintStrategy';

const STRATEGIES: Record<
  SourceType,
  (
    sourceId: string,
    entities: PropagatedEntitiesGroup,
    context: SyncContext,
    erdStore: ErdStore,
    idMap: Map<string, IdMappingWithType>,
  ) => void
> = {
  RELATIONSHIP: relationshipStrategy,
  CONSTRAINT: constraintStrategy,
};

export function syncPropagatedEntities(
  propagated: AffectedMappingResponse['propagated'],
  context: SyncContext,
  erdStore: ErdStore,
  idMap: Map<string, IdMappingWithType>,
): void {
  const bySource = groupBySourceId(propagated);

  Object.entries(bySource).forEach(([sourceId, entities]) => {
    const strategy = STRATEGIES[entities.sourceType];
    if (strategy) {
      strategy(sourceId, entities, context, erdStore, idMap);
    } else {
      console.warn(`Unknown source type: ${entities.sourceType}`);
    }
  });
}

function groupBySourceId(
  propagated: AffectedMappingResponse['propagated'],
): Record<string, PropagatedEntitiesGroup> {
  const grouped: Record<string, PropagatedEntitiesGroup> = {};

  const initGroup = (sourceId: string, sourceType: string) => {
    if (!grouped[sourceId]) {
      grouped[sourceId] = {
        sourceType: sourceType as SourceType,
        columns: [],
        relationshipColumns: [],
        constraints: [],
        constraintColumns: [],
      };
    }
  };

  propagated.columns.forEach((col) => {
    initGroup(col.sourceId, col.sourceType);
    grouped[col.sourceId].columns.push(col);
  });

  propagated.relationshipColumns.forEach((rc) => {
    initGroup(rc.sourceId, rc.sourceType);
    grouped[rc.sourceId].relationshipColumns.push(rc);
  });

  propagated.constraints.forEach((con) => {
    initGroup(con.sourceId, con.sourceType);
    grouped[con.sourceId].constraints.push(con);
  });

  propagated.constraintColumns.forEach((cc) => {
    initGroup(cc.sourceId, cc.sourceType);
    grouped[cc.sourceId].constraintColumns.push(cc);
  });

  return grouped;
}
