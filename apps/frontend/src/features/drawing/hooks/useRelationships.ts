import type { Connection, Edge, EdgeChange } from '@xyflow/react';
import { useState, useRef, useEffect } from 'react';
import { applyEdgeChanges } from '@xyflow/react';
import { autorun } from 'mobx';
import { toast } from 'sonner';
import { ErdStore } from '@/store';
import type { RelationshipConfig } from '../types';
import {
  RELATIONSHIP_TYPES,
  type RelationshipExtra,
  type Point,
} from '../types';
import {
  convertRelationshipsToEdges,
  createRelationshipFromConnection,
  findRelationshipInSchema,
} from '../utils/relationshipHelpers';
import * as relationshipService from '../services/relationship.service';

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const erdStore = ErdStore.getInstance();
  const [selectedRelationship, setSelectedRelationship] = useState<
    string | null
  >(null);
  const relationshipReconnectSuccessful = useRef(true);

  const updateRelationshipControlPoint = async (
    relationshipId: string,
    controlPoint1: Point,
    controlPoint2?: Point,
  ) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) return;

    const currentRelationship = findRelationshipInSchema(
      selectedSchema,
      relationshipId,
    );
    if (!currentRelationship) return;

    const currentExtra =
      typeof currentRelationship.extra === 'string'
        ? (JSON.parse(currentRelationship.extra) as RelationshipExtra)
        : ((currentRelationship.extra || {}) as RelationshipExtra);

    const updatedExtra: RelationshipExtra = {
      ...currentExtra,
      controlPoint1,
    };

    if (controlPoint2) {
      updatedExtra.controlPoint2 = controlPoint2;
    }

    try {
      await relationshipService.updateRelationshipExtra(
        selectedSchemaId,
        relationshipId,
        updatedExtra,
      );
    } catch (error) {
      toast.error('Failed to update relationship control point');
      console.error(error);
    }
  };

  const getRelationships = (): Edge[] => {
    const selectedSchema = erdStore.selectedSchema;
    if (!selectedSchema) return [];

    const edges = convertRelationshipsToEdges(selectedSchema);

    return edges.map((edge) => ({
      ...edge,
      data: {
        ...edge.data,
        onControlPointDragEnd: updateRelationshipControlPoint,
      },
    }));
  };

  const [relationships, setRelationships] = useState<Edge[]>(() =>
    getRelationships(),
  );

  useEffect(() => {
    const dispose = autorun(() => {
      const state = erdStore.erdState;
      if (state.state === 'loaded') {
        setRelationships(getRelationships());
      }
    });

    return () => dispose();
  }, []);

  const onConnect = async (params: Connection) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (
      !selectedSchemaId ||
      !selectedSchema ||
      !params.source ||
      !params.target
    ) {
      return;
    }

    const newRelationship = createRelationshipFromConnection({
      schema: selectedSchema,
      connection: params,
      relationshipConfig,
    });

    if (newRelationship) {
      try {
        await relationshipService.createRelationship(
          selectedSchemaId,
          newRelationship.srcTableId,
          newRelationship.tgtTableId,
          newRelationship.name,
          newRelationship.kind,
          newRelationship.cardinality,
          newRelationship.onDelete,
          newRelationship.onUpdate,
          newRelationship.fkEnforced,
          newRelationship.columns.map((col) => ({
            fkColumnId: col.fkColumnId,
            refColumnId: col.refColumnId,
            seqNo: col.seqNo,
          })),
          JSON.stringify(newRelationship.extra),
        );
      } catch (error) {
        toast.error('Failed to create relationship');
        console.error(error);
      }
    }
  };

  const onRelationshipsChange = (changes: EdgeChange[]) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    if (!selectedSchemaId) return;

    changes
      .filter((change) => change.type === 'remove')
      .forEach(async (change) => {
        try {
          await relationshipService.deleteRelationship(
            selectedSchemaId,
            change.id,
          );
        } catch (error) {
          toast.error('Failed to delete relationship');
          console.error(error);
        }
      });

    setRelationships((edges) => applyEdgeChanges(changes, edges) as Edge[]);
  };

  const onRelationshipClick = (event: React.MouseEvent, relationship: Edge) => {
    event.stopPropagation();
    setSelectedRelationship(relationship.id);
  };

  const onReconnectStart = () => {
    relationshipReconnectSuccessful.current = false;
  };

  const onReconnect = async (
    oldRelationship: Edge,
    newConnection: Connection,
  ) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) return;

    relationshipReconnectSuccessful.current = true;

    try {
      await relationshipService.deleteRelationship(
        selectedSchemaId,
        oldRelationship.id,
      );

      const newRelationship = createRelationshipFromConnection({
        schema: selectedSchema,
        connection: newConnection,
        relationshipConfig,
      });

      if (newRelationship) {
        await relationshipService.createRelationship(
          selectedSchemaId,
          newRelationship.srcTableId,
          newRelationship.tgtTableId,
          newRelationship.name,
          newRelationship.kind,
          newRelationship.cardinality,
          newRelationship.onDelete,
          newRelationship.onUpdate,
          newRelationship.fkEnforced,
          newRelationship.columns.map((col) => ({
            fkColumnId: col.fkColumnId,
            refColumnId: col.refColumnId,
            seqNo: col.seqNo,
          })),
          JSON.stringify(newRelationship.extra),
        );
      }
    } catch (error) {
      toast.error('Failed to reconnect relationship');
      console.error(error);
    }
  };

  const onReconnectEnd = async (
    _: MouseEvent | TouchEvent,
    relationship: Edge,
  ) => {
    const selectedSchemaId = erdStore.selectedSchemaId;

    if (!selectedSchemaId) return;

    if (!relationshipReconnectSuccessful.current) {
      try {
        await relationshipService.deleteRelationship(
          selectedSchemaId,
          relationship.id,
        );
      } catch (error) {
        toast.error('Failed to delete relationship');
        console.error(error);
      }
    }
    relationshipReconnectSuccessful.current = true;
  };

  const updateRelationshipConfig = async (
    relationshipId: string,
    config: RelationshipConfig,
  ) => {
    const selectedSchemaId = erdStore.selectedSchemaId;
    const selectedSchema = erdStore.selectedSchema;

    if (!selectedSchemaId || !selectedSchema) {
      toast.error('No schema selected');
      return;
    }

    const currentRelationship = findRelationshipInSchema(
      selectedSchema,
      relationshipId,
    );
    if (!currentRelationship) {
      toast.error('Relationship not found');
      return;
    }

    const typeConfig = RELATIONSHIP_TYPES[config.type];
    const newKind = config.isNonIdentifying ? 'NON_IDENTIFYING' : 'IDENTIFYING';
    const newCardinality = typeConfig.cardinality;

    const kindChanged = currentRelationship.kind !== newKind;
    const cardinalityChanged = currentRelationship.cardinality !== newCardinality;

    try {
      if (kindChanged) {
        await relationshipService.updateRelationshipKind(
          selectedSchemaId,
          relationshipId,
          newKind,
        );
      } else if (cardinalityChanged) {
        await relationshipService.updateRelationshipCardinality(
          selectedSchemaId,
          relationshipId,
          newCardinality,
        );
      }
    } catch (error) {
      toast.error('Failed to update relationship configuration');
      console.error(error);
    }
  };

  const deleteRelationship = async (relationshipId: string) => {
    const selectedSchemaId = erdStore.selectedSchemaId;

    if (!selectedSchemaId) {
      toast.error('No schema selected');
      return;
    }

    try {
      await relationshipService.deleteRelationship(
        selectedSchemaId,
        relationshipId,
      );
      setSelectedRelationship(null);
    } catch (error) {
      toast.error('Failed to delete relationship');
      console.error(error);
    }
  };

  const changeRelationshipName = async (
    relationshipId: string,
    newName: string,
  ) => {
    const selectedSchemaId = erdStore.selectedSchemaId;

    if (!selectedSchemaId) {
      toast.error('No schema selected');
      return;
    }

    try {
      await relationshipService.updateRelationshipName(
        selectedSchemaId,
        relationshipId,
        newName,
      );
    } catch (error) {
      toast.error('Failed to update relationship name');
      console.error(error);
    }
  };

  return {
    relationships,
    selectedRelationship,
    onConnect,
    onRelationshipsChange,
    onRelationshipClick,
    onReconnectStart,
    onReconnect,
    onReconnectEnd,
    updateRelationshipConfig,
    deleteRelationship,
    changeRelationshipName,
    setSelectedRelationship,
  };
};
