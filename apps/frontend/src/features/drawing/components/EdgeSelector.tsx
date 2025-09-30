import type { Edge } from '@xyflow/react';
import { Button } from '@/components';
import { RelationshipSelector } from './RelationshipSelector';
import { type RelationshipConfig, isRelationshipType } from '../types';

interface EdgeSelectorProps {
  selectedEdge: string;
  edges: Edge[];
  onRelationshipChange: (edgeId: string, config: RelationshipConfig) => void;
  onClose: () => void;
}

export const EdgeSelector = ({
  selectedEdge,
  edges,
  onRelationshipChange,
  onClose,
}: EdgeSelectorProps) => {
  const edge = edges.find((e) => e.id === selectedEdge);

  const relationshipType = edge?.data?.relationshipType;
  const isDashed =
    typeof edge?.data?.isDashed === 'boolean' ? edge.data.isDashed : false;
  const controlPointX =
    typeof edge?.data?.controlPointX === 'number'
      ? edge.data.controlPointX
      : undefined;
  const controlPointY =
    typeof edge?.data?.controlPointY === 'number'
      ? edge.data.controlPointY
      : undefined;

  const currentConfig: RelationshipConfig = {
    type: isRelationshipType(relationshipType)
      ? relationshipType
      : 'one-to-many',
    isDashed,
    controlPointX,
    controlPointY,
  };

  const handleConfigChange = (newConfig: RelationshipConfig) => {
    onRelationshipChange(selectedEdge, newConfig);
  };

  return (
    <div className="absolute top-4 left-4 z-10 bg-schemafy-bg p-3 rounded-lg shadow-md border border-schemafy-light-gray space-y-3">
      <div className="text-sm font-medium text-schemafy-text">
        Edit Relationship
      </div>

      <RelationshipSelector
        config={currentConfig}
        onChange={handleConfigChange}
      />

      <Button onClick={onClose} fullWidth>
        Close
      </Button>
    </div>
  );
};
