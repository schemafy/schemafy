import { useCallback, useEffect, useRef, useState } from 'react';
import type { Connection, Edge, EdgeChange } from '@xyflow/react';
import { toast } from 'sonner';
import { useSelectedSchema } from '../contexts';
import { useSchemaSnapshots } from './useSchemaSnapshots';
import {
  useChangeRelationshipCardinality,
  useChangeRelationshipExtra,
  useChangeRelationshipKind,
  useChangeRelationshipName,
  useCreateRelationshipWithExtra,
  useDeleteRelationship,
} from './useRelationshipMutations';
import {
  convertSnapshotsToEdges,
  findRelationshipById,
  parseRelationshipExtra,
  validateConnection,
} from '../utils/relationshipHelpers';
import type { Point, RelationshipConfig, RelationshipExtra } from '../types';
import { RELATIONSHIP_TYPES } from '../types';
import { useLatest } from './useLatest';
import type {
  RelationshipSnapshotResponse,
  TableSnapshotResponse,
} from '../api';
import { collaborationStore } from '@/store/collaboration.store';
import { previewStore } from '@/store/preview.store';
import type { PreviewEntry, RelationshipExtraPreviewEntry } from '@/store';
import { useThrottledCallback } from '@/hooks/useThrottledCallback';

const RELATIONSHIP_EXTRA_PREVIEW_THROTTLE_MS = 50;
const RELATIONSHIP_EXTRA_PREVIEW_CLEAR_DELAY_MS = 500;

const attachControlPointHandler = (
  edge: Edge,
  onControlPointDragEnd: (
    relationshipId: string,
    controlPoint1: Point,
    controlPoint2?: Point,
  ) => void,
  onControlPointDrag: (
    relationshipId: string,
    controlPoint1: Point,
    controlPoint2?: Point,
  ) => void,
) =>
  ({
    ...edge,
    data: {
      ...edge.data,
      onControlPointDragEnd,
      onControlPointDrag,
    },
  }) as Edge;

const isRelationshipExtraPreview = (
  entry: PreviewEntry,
): entry is RelationshipExtraPreviewEntry =>
  entry.kind === 'RELATIONSHIP_EXTRA';

const getRelationshipExtraPreviewMap = (schemaId: string) => {
  const previewExtras = new Map<string, RelationshipExtra>();

  for (const entry of previewStore.previews.values()) {
    if (isRelationshipExtraPreview(entry) && entry.schemaId === schemaId) {
      previewExtras.set(entry.relationshipId, entry.extra);
    }
  }

  return previewExtras;
};

const applyRelationshipExtraPreviews = (
  edges: Edge[],
  previewExtras: Map<string, RelationshipExtra>,
) => {
  if (previewExtras.size === 0) return edges;

  return edges.map((edge) => {
    const extra = previewExtras.get(edge.id);
    if (!extra) return edge;

    return {
      ...edge,
      sourceHandle: extra.fkHandle,
      targetHandle: extra.pkHandle,
      data: {
        ...edge.data,
        controlPoint1: extra.controlPoint1,
        controlPoint2: extra.controlPoint2,
      },
    } as Edge;
  });
};

const collectRelationshipSnapshots = (
  snapshots: Record<string, TableSnapshotResponse>,
) => {
  const relationshipSnapshots = new Map<string, RelationshipSnapshotResponse>();
  const tableIds = new Set(Object.keys(snapshots));

  Object.values(snapshots).forEach((snapshot) => {
    snapshot.relationships.forEach((relationshipSnapshot) => {
      const { relationship } = relationshipSnapshot;

      if (
        !tableIds.has(relationship.fkTableId) ||
        !tableIds.has(relationship.pkTableId)
      ) {
        return;
      }

      relationshipSnapshots.set(relationship.id, relationshipSnapshot);
    });
  });

  return relationshipSnapshots;
};

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const { selectedSchemaId } = useSelectedSchema();
  const { data: schemaSnapshots } = useSchemaSnapshots(selectedSchemaId);
  const snapshotsData = schemaSnapshots.snapshots;
  const relationshipExtraPreviewEntries = [
    ...previewStore.previews.values(),
  ].filter(
    (entry): entry is RelationshipExtraPreviewEntry =>
      isRelationshipExtraPreview(entry) && entry.schemaId === selectedSchemaId,
  );
  const relationshipExtraPreviewKey = relationshipExtraPreviewEntries
    .map((entry) => `${entry.relationshipId}:${JSON.stringify(entry.extra)}`)
    .sort()
    .join('|');
  const snapshotsRef = useLatest(snapshotsData);
  const relationshipConfigRef = useLatest(relationshipConfig);
  const previousSnapshotsRef = useRef<Record<
    string,
    TableSnapshotResponse
  > | null>(null);
  const previousSchemaIdRef = useRef(selectedSchemaId);

  const { mutate: createRelationshipWithExtra } =
    useCreateRelationshipWithExtra(selectedSchemaId);
  const { mutate: changeRelationshipName } =
    useChangeRelationshipName(selectedSchemaId);
  const { mutate: changeRelationshipExtra } =
    useChangeRelationshipExtra(selectedSchemaId);
  const { mutate: changeRelationshipKind } =
    useChangeRelationshipKind(selectedSchemaId);
  const { mutate: changeRelationshipCardinality } =
    useChangeRelationshipCardinality(selectedSchemaId);
  const { mutate: deleteRelationshipMutation } =
    useDeleteRelationship(selectedSchemaId);

  const [selectedRelationship, setSelectedRelationship] = useState<
    string | null
  >(null);
  const justFinishedControlPointDrag = useRef(false);
  const clearPreviewTimerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (clearPreviewTimerRef.current !== null) {
        window.clearTimeout(clearPreviewTimerRef.current);
      }
    };
  }, []);

  const updateExtra = useCallback(
    (
      relationshipId: string,
      updater: (extra: RelationshipExtra) => RelationshipExtra,
    ) => {
      const snapshot = findRelationshipById(
        snapshotsRef.current,
        relationshipId,
      );
      if (!snapshot) return;

      const currentExtra = parseRelationshipExtra(snapshot.relationship.extra);
      changeRelationshipExtra({
        relationshipId,
        data: { extra: updater(currentExtra) },
      });
    },
    [changeRelationshipExtra, snapshotsRef],
  );

  const updateRelationshipControlPoint = useCallback(
    (relationshipId: string, controlPoint1: Point, controlPoint2?: Point) => {
      const snapshot = findRelationshipById(
        snapshotsRef.current,
        relationshipId,
      );
      if (!snapshot) return;

      justFinishedControlPointDrag.current = true;
      requestAnimationFrame(() => {
        justFinishedControlPointDrag.current = false;
      });

      updateExtra(relationshipId, (extra: RelationshipExtra) => ({
        ...extra,
        controlPoint1,
        controlPoint2: controlPoint2 ?? undefined,
      }));

      collaborationStore.sendRelationshipExtraPreview(
        selectedSchemaId,
        relationshipId,
        {
          ...parseRelationshipExtra(snapshot.relationship.extra),
          controlPoint1,
          controlPoint2: controlPoint2 ?? undefined,
        },
      );

      if (clearPreviewTimerRef.current !== null) {
        window.clearTimeout(clearPreviewTimerRef.current);
      }
      clearPreviewTimerRef.current = window.setTimeout(() => {
        collaborationStore.sendRelationshipExtraPreview(
          selectedSchemaId,
          relationshipId,
          null,
        );
        clearPreviewTimerRef.current = null;
      }, RELATIONSHIP_EXTRA_PREVIEW_CLEAR_DELAY_MS);
    },
    [selectedSchemaId, snapshotsRef, updateExtra],
  );

  const sendRelationshipExtraPreview = useThrottledCallback(
    (relationshipId: string, controlPoint1: Point, controlPoint2?: Point) => {
      const snapshot = findRelationshipById(
        snapshotsRef.current,
        relationshipId,
      );
      if (!snapshot) return;

      const currentExtra = parseRelationshipExtra(snapshot.relationship.extra);

      collaborationStore.sendRelationshipExtraPreview(
        selectedSchemaId,
        relationshipId,
        {
          ...currentExtra,
          controlPoint1,
          controlPoint2: controlPoint2 ?? undefined,
        },
      );
    },
    RELATIONSHIP_EXTRA_PREVIEW_THROTTLE_MS,
  );

  const [relationships, setRelationships] = useState<Edge[]>(() =>
    applyRelationshipExtraPreviews(
      convertSnapshotsToEdges(snapshotsData).map((edge) =>
        attachControlPointHandler(
          edge,
          updateRelationshipControlPoint,
          sendRelationshipExtraPreview,
        ),
      ),
      getRelationshipExtraPreviewMap(selectedSchemaId),
    ),
  );

  useEffect(() => {
    const previousSnapshots = previousSnapshotsRef.current;
    const schemaChanged = previousSchemaIdRef.current !== selectedSchemaId;

    previousSnapshotsRef.current = snapshotsData;
    previousSchemaIdRef.current = selectedSchemaId;
    const relationshipExtraPreviewMap =
      getRelationshipExtraPreviewMap(selectedSchemaId);

    setRelationships((previousRelationships) => {
      if (!previousSnapshots || schemaChanged) {
        return applyRelationshipExtraPreviews(
          convertSnapshotsToEdges(snapshotsData).map((edge) =>
            attachControlPointHandler(
              edge,
              updateRelationshipControlPoint,
              sendRelationshipExtraPreview,
            ),
          ),
          relationshipExtraPreviewMap,
        );
      }

      const previousEdgesById = new Map(
        previousRelationships.map((relationship) => [
          relationship.id,
          relationship,
        ]),
      );
      const previousRelationshipSnapshots =
        collectRelationshipSnapshots(previousSnapshots);
      const nextRelationshipSnapshots =
        collectRelationshipSnapshots(snapshotsData);

      nextRelationshipSnapshots.forEach(
        (relationshipSnapshot, relationshipId) => {
          const previousRelationshipSnapshot =
            previousRelationshipSnapshots.get(relationshipId);

          if (
            !previousRelationshipSnapshot ||
            previousRelationshipSnapshot !== relationshipSnapshot
          ) {
            const nextEdge = convertSnapshotsToEdges({
              [relationshipSnapshot.relationship.fkTableId]:
                snapshotsData[relationshipSnapshot.relationship.fkTableId],
              [relationshipSnapshot.relationship.pkTableId]:
                snapshotsData[relationshipSnapshot.relationship.pkTableId],
            }).find((edge) => edge.id === relationshipId);

            if (nextEdge) {
              previousEdgesById.set(
                relationshipId,
                attachControlPointHandler(
                  nextEdge,
                  updateRelationshipControlPoint,
                  sendRelationshipExtraPreview,
                ),
              );
            }
          }
        },
      );

      return applyRelationshipExtraPreviews(
        Array.from(nextRelationshipSnapshots.keys()).map(
          (relationshipId) => previousEdgesById.get(relationshipId)!,
        ),
        relationshipExtraPreviewMap,
      );
    });
  }, [
    snapshotsData,
    selectedSchemaId,
    updateRelationshipControlPoint,
    sendRelationshipExtraPreview,
    relationshipExtraPreviewKey,
  ]);

  useEffect(() => {
    setSelectedRelationship((currentSelectedRelationship) => {
      if (!currentSelectedRelationship) return currentSelectedRelationship;

      const relationshipExists = relationships.some(
        (relationship) => relationship.id === currentSelectedRelationship,
      );

      return relationshipExists ? currentSelectedRelationship : null;
    });
  }, [relationships]);

  const createRelationshipFromValidation = useCallback(
    (
      validation: {
        fkTableId: string;
        pkTableId: string;
      },
      connection: Connection,
    ) => {
      const currentRelationshipConfig = relationshipConfigRef.current;
      const typeConfig = RELATIONSHIP_TYPES[currentRelationshipConfig.type];
      const kind = currentRelationshipConfig.isNonIdentifying
        ? 'NON_IDENTIFYING'
        : 'IDENTIFYING';

      const isSameDirection = connection.source === validation.fkTableId;
      const fkHandle =
        (isSameDirection ? connection.sourceHandle : connection.targetHandle) ??
        undefined;
      const pkHandle =
        (isSameDirection ? connection.targetHandle : connection.sourceHandle) ??
        undefined;
      const extra: RelationshipExtra = {
        fkHandle,
        pkHandle,
      };

      createRelationshipWithExtra({
        request: {
          fkTableId: validation.fkTableId,
          pkTableId: validation.pkTableId,
          kind,
          cardinality: typeConfig.cardinality,
        },
        extra,
      });
    },
    [createRelationshipWithExtra, relationshipConfigRef],
  );

  const onConnect = useCallback(
    (params: Connection) => {
      const validation = validateConnection({
        snapshots: snapshotsRef.current,
        connection: params,
      });

      if (!validation.isValid) {
        if (validation.error) {
          toast.error(validation.error);
        }
        return;
      }

      createRelationshipFromValidation(validation, params);
    },
    [createRelationshipFromValidation, snapshotsRef],
  );

  const onRelationshipsChange = useCallback(
    (changes: EdgeChange[]) => {
      changes
        .filter((change) => change.type === 'remove')
        .forEach((change) => {
          deleteRelationshipMutation(change.id);
        });
    },
    [deleteRelationshipMutation],
  );

  const onRelationshipClick = useCallback(
    (event: React.MouseEvent, relationship: Edge) => {
      if (justFinishedControlPointDrag.current) return;
      event.stopPropagation();
      setSelectedRelationship(relationship.id);
    },
    [],
  );

  const updateRelationshipConfig = useCallback(
    (relationshipId: string, config: RelationshipConfig) => {
      const relationshipSnapshot = findRelationshipById(
        snapshotsRef.current,
        relationshipId,
      );

      if (!relationshipSnapshot) {
        toast.error('Relationship not found');
        return;
      }

      const { relationship } = relationshipSnapshot;
      const typeConfig = RELATIONSHIP_TYPES[config.type];
      const newKind = config.isNonIdentifying
        ? 'NON_IDENTIFYING'
        : 'IDENTIFYING';

      const needsKindUpdate = relationship.kind !== newKind;
      const needsCardinalityUpdate =
        relationship.cardinality !== typeConfig.cardinality;

      if (needsKindUpdate) {
        changeRelationshipKind(
          { relationshipId, data: { kind: newKind } },
          {
            onSuccess: () => {
              if (needsCardinalityUpdate) {
                changeRelationshipCardinality({
                  relationshipId,
                  data: { cardinality: typeConfig.cardinality },
                });
              }
            },
          },
        );
      } else if (needsCardinalityUpdate) {
        changeRelationshipCardinality({
          relationshipId,
          data: { cardinality: typeConfig.cardinality },
        });
      }
    },
    [changeRelationshipCardinality, changeRelationshipKind, snapshotsRef],
  );

  const deleteRelationship = useCallback(
    (relationshipId: string) => {
      deleteRelationshipMutation(relationshipId);
      setSelectedRelationship(null);
    },
    [deleteRelationshipMutation],
  );

  const updateRelationshipName = useCallback(
    (relationshipId: string, newName: string) => {
      changeRelationshipName({
        relationshipId,
        data: { newName },
      });
    },
    [changeRelationshipName],
  );

  return {
    relationships,
    selectedRelationship,
    onConnect,
    onRelationshipsChange,
    onRelationshipClick,
    updateRelationshipConfig,
    deleteRelationship,
    updateRelationshipName,
    setSelectedRelationship,
  };
};
