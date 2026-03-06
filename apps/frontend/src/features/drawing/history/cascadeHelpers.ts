import type { CreateRelationshipRequest, MutationResponse, RelationshipResponse, TableSnapshotResponse } from '../api';

export function collectCascadeSnapshots(
  pkTableId: string,
  allSnapshots: Record<string, TableSnapshotResponse>,
  result: Record<string, TableSnapshotResponse>,
): void {
  const snapshot = allSnapshots[pkTableId];
  if (!snapshot) return;

  for (const relSnapshot of snapshot.relationships) {
    const {relationship} = relSnapshot;
    if (relationship.pkTableId !== pkTableId) continue;
    if (relationship.kind !== 'IDENTIFYING') continue;

    const fkTableId = relationship.fkTableId;
    if (result[fkTableId]) continue;

    const fkSnapshot = allSnapshots[fkTableId];
    if (!fkSnapshot) continue;

    result[fkTableId] = fkSnapshot;
    collectCascadeSnapshots(fkTableId, allSnapshots, result);
  }
}

export async function restoreCascadeRelationships(
  pkTableId: string,
  cascadeSnapshots: Record<string, TableSnapshotResponse>,
  currentSnapshots: Record<string, TableSnapshotResponse> | undefined,
  allAffectedTableIds: Set<string>,
  onCreateRelationship: (params: CreateRelationshipRequest) => Promise<MutationResponse<RelationshipResponse>>,
  visitedTableIds: Set<string> = new Set(),
): Promise<void> {
  if (visitedTableIds.has(pkTableId)) return;
  visitedTableIds.add(pkTableId);

  const pkSnapshot = cascadeSnapshots[pkTableId];
  if (!pkSnapshot) return;

  for (const relSnapshot of pkSnapshot.relationships) {
    const {relationship} = relSnapshot;
    if (relationship.pkTableId !== pkTableId) continue;

    const alreadyExists = currentSnapshots?.[pkTableId]?.relationships.some(
      (r) =>
        r.relationship.fkTableId === relationship.fkTableId &&
        r.relationship.pkTableId === pkTableId,
    );
    if (alreadyExists) continue;

    const relResult = await onCreateRelationship({
      fkTableId: relationship.fkTableId,
      pkTableId,
      kind: relationship.kind,
      cardinality: relationship.cardinality,
      extra: relationship.extra ?? undefined,
    });

    for (const id of relResult.affectedTableIds) {
      allAffectedTableIds.add(id);
    }

    if (relationship.kind === 'IDENTIFYING') {
      await restoreCascadeRelationships(
        relationship.fkTableId,
        cascadeSnapshots,
        currentSnapshots,
        allAffectedTableIds,
        onCreateRelationship,
        visitedTableIds,
      );
    }
  }
}
