import { ErdStore } from '@/store/erd.store';
import type {
  PropagatedEntitiesGroup,
  ServerResponse,
  SyncContext,
  SourceType,
} from '@/features/drawing/types';
import { relationshipStrategy } from './strategies/relationshipStrategy';
import { constraintStrategy } from './strategies/constraintStrategy';
import type { PropagationStrategy } from './strategies/types';

const STRATEGIES: Record<SourceType, PropagationStrategy> = {
  RELATIONSHIP: relationshipStrategy,
  CONSTRAINT: constraintStrategy,
};

export function syncPropagatedEntities(
  propagated: NonNullable<NonNullable<ServerResponse['result']>['propagated']>,
  context: SyncContext,
): void {
  const erdStore = ErdStore.getInstance();
  const bySource = groupBySourceId(propagated);

  Object.entries(bySource).forEach(([sourceId, entities]) => {
    const strategy = STRATEGIES[entities.sourceType];
    if (strategy) {
      strategy(sourceId, entities, context, erdStore);
    } else {
      console.warn(`Unknown source type: ${entities.sourceType}`);
    }
  });
}

function groupBySourceId(
  propagated: NonNullable<NonNullable<ServerResponse['result']>['propagated']>,
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
