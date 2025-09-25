import type { Edge } from '@xyflow/react';
import type { RelationshipType } from '../types';
import { RelationshipSelector } from './RelationshipSelector';
import { Button } from '@/components';

export const EdgeSelector = ({
  selectedEdge,
  edges,
  onRelationshipChange,
  onClose,
}: {
  selectedEdge: string;
  edges: Edge[];
  onRelationshipChange: (edgeId: string, type: RelationshipType) => void;
  onClose: () => void;
}) => {
  const edge = edges.find((e) => e.id === selectedEdge);
  const currentType =
    (edge?.data?.relationshipType as RelationshipType) || 'one-to-many';

  return (
    <div className="absolute top-4 right-4 z-10 bg-white p-3 rounded-lg shadow-md border">
      <div className="mb-2">
        <RelationshipSelector
          onSelect={(type) => onRelationshipChange(selectedEdge, type)}
          currentType={currentType}
        />
      </div>
      <Button onClick={onClose} variant="outline">
        Close
      </Button>
    </div>
  );
};
