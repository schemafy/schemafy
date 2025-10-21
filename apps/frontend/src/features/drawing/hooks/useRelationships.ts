import { addEdge, applyEdgeChanges, reconnectEdge, type Connection, type Edge, type EdgeChange } from '@xyflow/react';
import { useState, useRef } from 'react';
import { RELATIONSHIP_TYPES, RELATIONSHIP_STYLE_TYPES, type RelationshipConfig } from '../types';

const getRelationshipStyle = (isDashed: boolean = false) => {
  return isDashed ? RELATIONSHIP_STYLE_TYPES.dashed : RELATIONSHIP_STYLE_TYPES.solid;
};

export const useRelationships = (relationshipConfig: RelationshipConfig) => {
  const [relationships, setRelationships] = useState<Edge[]>([]);
  const [selectedRelationship, setSelectedRelationship] = useState<string | null>(null);
  const relationshipReconnectSuccessful = useRef(true);

  const createRelationship = (params: Connection): Edge => {
    const baseConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
    const style = getRelationshipStyle(relationshipConfig.isDashed);

    return {
      ...params,
      id: `relationship_${Date.now()}`,
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
        relationshipType: relationshipConfig.type,
        isDashed: relationshipConfig.isDashed,
      },
    } as Edge;
  };

  const onConnect = (params: Connection) => {
    const newRelationship = createRelationship(params);
    setRelationships((rels) => addEdge(newRelationship, rels));
  };

  const onRelationshipsChange = (changes: EdgeChange[]) => {
    setRelationships((rels) => applyEdgeChanges(changes, rels));
  };

  const onRelationshipClick = (event: React.MouseEvent, relationship: Edge) => {
    event.stopPropagation();
    setSelectedRelationship(relationship.id);
  };

  const onReconnectStart = () => {
    relationshipReconnectSuccessful.current = false;
  };

  const onReconnect = (oldRelationship: Edge, newConnection: Connection) => {
    relationshipReconnectSuccessful.current = true;
    setRelationships((rels) => reconnectEdge(oldRelationship, newConnection, rels));
  };

  const onReconnectEnd = (_: MouseEvent | TouchEvent, relationship: Edge) => {
    if (!relationshipReconnectSuccessful.current) {
      setRelationships((rels) => rels.filter((r) => r.id !== relationship.id));
    }
    relationshipReconnectSuccessful.current = true;
  };

  const changeRelationshipConfig = (relationshipId: string, newConfig: RelationshipConfig) => {
    const baseConfig = RELATIONSHIP_TYPES[newConfig.type];
    const style = getRelationshipStyle(newConfig.isDashed);

    setRelationships((rels) =>
      rels.map((relationship) =>
        relationship.id === relationshipId
          ? {
              ...relationship,
              style,
              markerStart: baseConfig.markerStart,
              markerEnd: baseConfig.markerEnd,
              label: baseConfig.label,
              labelStyle: {
                fontSize: 12,
                fontWeight: 'bold',
                color: 'var(--color-schemafy-dark-gray)',
              },
              data: {
                ...relationship.data,
                relationshipType: newConfig.type,
                isDashed: newConfig.isDashed,
              },
            }
          : relationship,
      ),
    );
    setSelectedRelationship(null);
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
    changeRelationshipConfig,
    setSelectedRelationship,
  };
};
