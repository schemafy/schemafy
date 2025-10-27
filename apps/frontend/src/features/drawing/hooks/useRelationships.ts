import { type Connection, type Edge, type EdgeChange } from '@xyflow/react';
import { useState, useRef, useMemo, useCallback } from 'react';
import { ulid } from 'ulid';
import { ErdStore } from '@/store';
import type { Relationship } from '@schemafy/validator';
import { RELATIONSHIP_TYPES, RELATIONSHIP_STYLE_TYPES, type RelationshipConfig, type RelationshipType } from '../types';

const getRelationshipType = (cardinality: string): RelationshipType => {
  return cardinality === '1:N' ? 'one-to-many' : 'one-to-one';
};

const getRelationshipStyle = (isNonIdentifying: boolean) => {
  return isNonIdentifying ? RELATIONSHIP_STYLE_TYPES.dashed : RELATIONSHIP_STYLE_TYPES.solid;
};

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const erdStore = ErdStore.getInstance();
  const [selectedRelationship, setSelectedRelationship] = useState<string | null>(null);
  const relationshipReconnectSuccessful = useRef(true);

  const relationships = useMemo<Edge[]>(() => {
    const { erdState, selectedSchemaId } = erdStore;

    if (erdState.state !== 'loaded' || !selectedSchemaId) {
      return [];
    }

    const schema = erdState.database.schemas.find((s) => s.id === selectedSchemaId);
    if (!schema) {
      return [];
    }

    const allRelationships: Edge[] = [];

    schema.tables.forEach((table) => {
      table.relationships.forEach((rel) => {
        const relationshipType = getRelationshipType(rel.cardinality);
        const baseConfig = RELATIONSHIP_TYPES[relationshipType];
        const isNonIdentifying = rel.kind === 'NON_IDENTIFYING';
        const style = getRelationshipStyle(isNonIdentifying);
        const extra = (rel.extra || {}) as {
          sourceHandle?: string;
          targetHandle?: string;
          controlPointX?: number;
          controlPointY?: number;
        };

        const edge: Edge = {
          id: rel.id,
          source: rel.srcTableId,
          target: rel.tgtTableId,
          sourceHandle: extra.sourceHandle,
          targetHandle: extra.targetHandle,
          type: 'customSmoothStep',
          style,
          markerStart: baseConfig.markerStart,
          markerEnd: baseConfig.markerEnd,
          label: baseConfig.label,
          labelStyle: {
            fontSize: 12,
            fontWeight: 'bold',
            color: style.stroke,
          },
          data: {
            relationshipType,
            isNonIdentifying,
            controlPointX: extra.controlPointX,
            controlPointY: extra.controlPointY,
            dbRelationship: rel,
          },
        } as Edge;

        allRelationships.push(edge);
      });
    });

    return allRelationships;
  }, [erdStore.erdState, erdStore.selectedSchemaId]);

  const onConnect = useCallback(
    (params: Connection) => {
      const { selectedSchemaId, erdState, createRelationship } = erdStore;

      if (!selectedSchemaId || !params.source || !params.target || erdState.state !== 'loaded') {
        return;
      }

      const schema = erdState.database.schemas.find((s) => s.id === selectedSchemaId);
      if (!schema) return;

      // Source table 찾기 - relationship은 source table의 relationships 배열에 저장됨
      const sourceTable = schema.tables.find((t) => t.id === params.source);
      if (!sourceTable) {
        console.error('Source table not found');
        return;
      }

      // Target table의 PK는 참조용으로만 사용
      const targetTable = schema.tables.find((t) => t.id === params.target);
      if (!targetTable) {
        console.error('Target table not found');
        return;
      }

      const targetPk = targetTable.constraints.find((c) => c.kind === 'PRIMARY_KEY');
      const targetPkColId = targetPk?.columns[0]?.columnId;

      if (!targetPkColId) {
        console.error('Target table must have a primary key to create a relationship');
        return;
      }

      const relId = ulid();
      const typeConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
      const kind = relationshipConfig.isNonIdentifying ? 'NON_IDENTIFYING' : 'IDENTIFYING';

      const fkColumnId = ulid();

      const newRelationship: Relationship = {
        id: relId,
        srcTableId: params.source,
        tgtTableId: params.target,
        name: `fk_${params.source}_${params.target}`,
        kind,
        cardinality: typeConfig.cardinality,
        onDelete: 'CASCADE',
        onUpdate: 'CASCADE',
        fkEnforced: false,
        columns: [
          {
            id: ulid(),
            relationshipId: relId,
            fkColumnId,
            refColumnId: targetPkColId,
            seqNo: 1,
          },
        ],
        extra: {
          sourceHandle: params.sourceHandle,
          targetHandle: params.targetHandle,
          controlPointX: relationshipConfig.controlPointX,
          controlPointY: relationshipConfig.controlPointY,
        },
      };

      // Source table의 relationships 배열에 추가
      createRelationship(selectedSchemaId, newRelationship);
      console.log(erdStore.erdState);
    },
    [erdStore, relationshipConfig],
  );

  const onRelationshipsChange = useCallback(
    (changes: EdgeChange[]) => {
      const { selectedSchemaId, deleteRelationship } = erdStore;

      if (!selectedSchemaId) return;

      changes.forEach((change) => {
        if (change.type === 'remove') {
          deleteRelationship(selectedSchemaId, change.id);
        }
      });
    },
    [erdStore],
  );

  const onRelationshipClick = useCallback((event: React.MouseEvent, relationship: Edge) => {
    event.stopPropagation();
    setSelectedRelationship(relationship.id);
  }, []);

  const onReconnectStart = useCallback(() => {
    relationshipReconnectSuccessful.current = false;
  }, []);

  const onReconnect = useCallback(
    (oldRelationship: Edge, newConnection: Connection) => {
      const { selectedSchemaId, erdState, deleteRelationship, createRelationship } = erdStore;

      if (!selectedSchemaId || erdState.state !== 'loaded') return;

      relationshipReconnectSuccessful.current = true;

      // 기존 relationship은 source table의 relationships 배열에서 삭제
      deleteRelationship(selectedSchemaId, oldRelationship.id);

      if (newConnection.source && newConnection.target) {
        const schema = erdState.database.schemas.find((s) => s.id === selectedSchemaId);
        if (!schema) return;

        // 새로운 source table 찾기
        const sourceTable = schema.tables.find((t) => t.id === newConnection.source);
        if (!sourceTable) {
          console.error('Source table not found');
          return;
        }

        // Target table의 PK는 참조용으로만 사용
        const targetTable = schema.tables.find((t) => t.id === newConnection.target);
        if (!targetTable) {
          console.error('Target table not found');
          return;
        }

        const targetPk = targetTable.constraints.find((c) => c.kind === 'PRIMARY_KEY');
        const targetPkColId = targetPk?.columns[0]?.columnId;

        if (!targetPkColId) {
          console.error('Target table must have a primary key to create a relationship');
          return;
        }

        const relId = ulid();
        const typeConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
        const kind = relationshipConfig.isNonIdentifying ? 'NON_IDENTIFYING' : 'IDENTIFYING';

        const fkColumnId = ulid();

        const newRelationship: Relationship = {
          id: relId,
          srcTableId: newConnection.source,
          tgtTableId: newConnection.target,
          name: `fk_${newConnection.source}_${newConnection.target}`,
          kind,
          cardinality: typeConfig.cardinality,
          onDelete: 'CASCADE',
          onUpdate: 'CASCADE',
          fkEnforced: false,
          columns: [
            {
              id: ulid(),
              relationshipId: relId,
              fkColumnId,
              refColumnId: targetPkColId,
              seqNo: 1,
            },
          ],
          extra: {
            sourceHandle: newConnection.sourceHandle,
            targetHandle: newConnection.targetHandle,
            controlPointX: relationshipConfig.controlPointX,
            controlPointY: relationshipConfig.controlPointY,
          },
        };

        // 새로운 source table의 relationships 배열에 추가
        createRelationship(selectedSchemaId, newRelationship);
      }
    },
    [erdStore, relationshipConfig],
  );

  const onReconnectEnd = useCallback(
    (_: MouseEvent | TouchEvent, relationship: Edge) => {
      const { selectedSchemaId, deleteRelationship } = erdStore;

      if (!selectedSchemaId) return;

      // Reconnect가 실패한 경우 source table의 relationships 배열에서 삭제
      if (!relationshipReconnectSuccessful.current) {
        deleteRelationship(selectedSchemaId, relationship.id);
      }
      relationshipReconnectSuccessful.current = true;
    },
    [erdStore],
  );

  const changeRelationshipConfig = () => {
    setSelectedRelationship(null);
  };

  const deleteRelationship = useCallback(
    (relationshipId: string) => {
      const { selectedSchemaId, deleteRelationship: deleteRel } = erdStore;

      if (!selectedSchemaId) {
        console.error('No schema selected');
        return;
      }

      // Source table의 relationships 배열에서 삭제
      deleteRel(selectedSchemaId, relationshipId);
      setSelectedRelationship(null);
    },
    [erdStore],
  );

  return {
    relationships,
    selectedRelationship,
    onConnect,
    onRelationshipsChange,
    onRelationshipClick,
    onReconnectStart,
    onReconnect,
    onReconnectEnd,
    changeRelationshipConfig,
    deleteRelationship,
    setSelectedRelationship,
  };
};
