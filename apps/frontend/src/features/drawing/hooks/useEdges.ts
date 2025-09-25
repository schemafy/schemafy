import {
  addEdge,
  applyEdgeChanges,
  type Connection,
  type Edge,
  type EdgeChange,
} from '@xyflow/react';
import { useState } from 'react';
import { RELATIONSHIP_TYPES, type RelationshipType } from '../types';

export const useEdges = (currentRelationshipType: RelationshipType) => {
  const [edges, setEdges] = useState<Edge[]>([]);
  const [selectedEdge, setSelectedEdge] = useState<string | null>(null);

  const createEdge = (params: Connection): Edge => {
    const relationshipConfig = RELATIONSHIP_TYPES[currentRelationshipType];

    return {
      ...params,
      id: `edge_${Date.now()}`,
      type: 'smoothstep',
      style: relationshipConfig.style,
      markerStart: relationshipConfig.markerStart,
      markerEnd: relationshipConfig.markerEnd,
      label: relationshipConfig.label,
      labelStyle: {
        fontSize: 12,
        fontWeight: 'bold',
        color: relationshipConfig.style.stroke,
      },
      data: { relationshipType: currentRelationshipType },
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

  const changeRelationshipType = (
    edgeId: string,
    newType: RelationshipType,
  ) => {
    const config = RELATIONSHIP_TYPES[newType];

    setEdges((eds) =>
      eds.map((edge) =>
        edge.id === edgeId
          ? {
              ...edge,
              style: config.style,
              markerStart: config.markerStart,
              markerEnd: config.markerEnd,
              label: config.label,
              labelStyle: {
                fontSize: 12,
                fontWeight: 'bold',
                color: config.style.stroke,
              },
              data: { ...edge.data, relationshipType: newType },
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
    changeRelationshipType,
    setSelectedEdge,
  };
};
