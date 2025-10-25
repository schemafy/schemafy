import type { Edge } from '@xyflow/react';
import { Button } from '@/components';
import { RelationshipSelector } from './RelationshipSelector';
import { type RelationshipConfig, isRelationshipType } from '../types';

interface RelationshipSelectorProps {
  selectedRelationship: string;
  relationships: Edge[];
  onRelationshipChange: (relationshipId: string, config: RelationshipConfig) => void;
  onRelationshipDelete: (relationshipId: string) => void;
  onClose: () => void;
}

export const EdgeSelector = ({
  selectedRelationship,
  relationships,
  onRelationshipChange,
  onRelationshipDelete,
  onClose,
}: RelationshipSelectorProps) => {
  const relationship = relationships.find((r) => r.id === selectedRelationship);

  const relationshipType = relationship?.data?.relationshipType;
  const isNonIdentifying =
    typeof relationship?.data?.isNonIdentifying === 'boolean' ? relationship.data.isNonIdentifying : false;
  const controlPointX =
    typeof relationship?.data?.controlPointX === 'number' ? relationship.data.controlPointX : undefined;
  const controlPointY =
    typeof relationship?.data?.controlPointY === 'number' ? relationship.data.controlPointY : undefined;

  const currentConfig: RelationshipConfig = {
    type: isRelationshipType(relationshipType) ? relationshipType : 'one-to-many',
    isNonIdentifying,
    controlPointX,
    controlPointY,
  };

  const handleConfigChange = (newConfig: RelationshipConfig) => {
    onRelationshipChange(selectedRelationship, newConfig);
  };

  const handleDelete = () => {
    onRelationshipDelete(selectedRelationship);
  };

  return (
    <div className="absolute top-4 left-4 z-10 bg-schemafy-bg p-3 rounded-lg shadow-md border border-schemafy-light-gray space-y-3">
      <div className="text-sm font-medium text-schemafy-text">Edit Relationship</div>

      <RelationshipSelector config={currentConfig} onChange={handleConfigChange} />

      <div className="flex gap-2">
        <Button onClick={handleDelete} variant="secondary" fullWidth>
          Delete
        </Button>
        <Button onClick={onClose} fullWidth>
          Close
        </Button>
      </div>
    </div>
  );
};
