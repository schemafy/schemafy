import {
  addEdge,
  applyEdgeChanges,
  reconnectEdge,
  type Connection,
  type Edge,
  type EdgeChange,
} from '@xyflow/react';
import { useState, useRef } from 'react';
import {
  RELATIONSHIP_TYPES,
  RELATIONSHIP_STYLE_TYPES,
  type RelationshipConfig,
} from '../types';

const getRelationshipStyle = (isDashed: boolean = false) => {
  return isDashed
    ? RELATIONSHIP_STYLE_TYPES.dashed
    : RELATIONSHIP_STYLE_TYPES.solid;
};

export const useEdges = (relationshipConfig: RelationshipConfig) => {
  const [edges, setEdges] = useState<Edge[]>([]);
  const [selectedEdge, setSelectedEdge] = useState<string | null>(null);
  const edgeReconnectSuccessful = useRef(true);

  const createEdge = (params: Connection): Edge => {
    const baseConfig = RELATIONSHIP_TYPES[relationshipConfig.type];
    const style = getRelationshipStyle(relationshipConfig.isDashed);

    return {
      ...params,
      id: `edge_${Date.now()}`,
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

  const onReconnectStart = () => {
    edgeReconnectSuccessful.current = false;
  };

  const onReconnect = (oldEdge: Edge, newConnection: Connection) => {
    edgeReconnectSuccessful.current = true;
    setEdges((els) => reconnectEdge(oldEdge, newConnection, els));
  };

  const onReconnectEnd = (_: MouseEvent | TouchEvent, edge: Edge) => {
    if (!edgeReconnectSuccessful.current) {
      setEdges((eds) => eds.filter((e) => e.id !== edge.id));
    }
    edgeReconnectSuccessful.current = true;
  };

  const changeRelationshipConfig = (
    edgeId: string,
    newConfig: RelationshipConfig,
  ) => {
    const baseConfig = RELATIONSHIP_TYPES[newConfig.type];
    const style = getRelationshipStyle(newConfig.isDashed);

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
                ...edge.data,
                relationshipType: newConfig.type,
                isDashed: newConfig.isDashed,
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
    onReconnectStart,
    onReconnect,
    onReconnectEnd,
    changeRelationshipConfig,
    setSelectedEdge,
  };
};
