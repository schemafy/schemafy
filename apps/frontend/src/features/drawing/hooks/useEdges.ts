import {
  addEdge,
  applyEdgeChanges,
  type Connection,
  type Edge,
  type EdgeChange,
} from '@xyflow/react';
import { useState } from 'react';
import { RELATIONSHIP_TYPES, type RelationshipConfig } from '../types';

const getRelationshipStyle = (isOptional: boolean = false) => {
  const baseStyle = {
    stroke: 'var(--color-schemafy-dark-gray)',
    strokeWidth: 2,
  };

  return isOptional ? { ...baseStyle, strokeDasharray: '5 5' } : baseStyle;
};

export const useEdges = (relationshipConfig: RelationshipConfig) => {
  const [edges, setEdges] = useState<Edge[]>([]);
  const [selectedEdge, setSelectedEdge] = useState<string | null>(null);

  const createEdge = (params: Connection): Edge => {
    const baseConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
    const style = getRelationshipStyle(relationshipConfig.isOptional);

    return {
      ...params,
      id: `edge_${Date.now()}`,
      type: 'smoothstep',
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
        isOptional: relationshipConfig.isOptional,
      },
    };
  };

  const onConnect = (params: Connection) => {
    const newEdge = createEdge(params);
    setEdges((eds) => addEdge(newEdge, eds));
  };

  const onEdgesChange = (changes: EdgeChange[]) => {
    setEdges((eds) => applyEdgeChanges(changes, eds));
  };

  const onEdgeClick = (event: React.MouseEvent, edge: Edge) => {
    event.stopPropagation();
    setSelectedEdge(edge.id);
  };

  const changeRelationshipConfig = (
    edgeId: string,
    newConfig: RelationshipConfig,
  ) => {
    const baseConfig = RELATIONSHIP_TYPES[newConfig.type];
    const style = getRelationshipStyle(newConfig.isOptional);

    setEdges((eds) =>
      eds.map((edge) =>
        edge.id === edgeId
          ? {
              ...edge,
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
                relationshipType: newConfig.type,
                isOptional: newConfig.isOptional,
              },
            }
          : edge,
      ),
    );
    setSelectedEdge(null);
  };

  return {
    edges,
    selectedEdge,
    onConnect,
    onEdgesChange,
    onEdgeClick,
    changeRelationshipConfig,
    setSelectedEdge,
  };
};
