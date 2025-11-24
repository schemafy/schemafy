import { useState, useEffect } from 'react';
import type { Edge } from '@xyflow/react';
import { Button } from '@/components';
import { RelationshipSelector } from './RelationshipSelector';
import {
  type RelationshipConfig,
  type RelationshipType,
  isRelationshipType,
} from '../types';

interface EdgeSelectorProps {
  selectedRelationship: string;
  relationships: Edge[];
  onRelationshipChange: (
    relationshipId: string,
    config: RelationshipConfig,
  ) => void;
  onRelationshipNameChange: (relationshipId: string, newName: string) => void;
  onRelationshipDelete: (relationshipId: string) => void;
  onClose: () => void;
}

interface RelationshipData {
  name: string;
  type: RelationshipType;
  isNonIdentifying: boolean;
  controlPointX?: number;
  controlPointY?: number;
}

const DEFAULT_RELATIONSHIP_DATA: RelationshipData = {
  name: '',
  type: 'one-to-many',
  isNonIdentifying: false,
  controlPointX: undefined,
  controlPointY: undefined,
};

const extractRelationshipData = (
  relationship: Edge | undefined,
): RelationshipData => {
  if (!relationship) return DEFAULT_RELATIONSHIP_DATA;

  const data = relationship.data || {};

  return {
    name: (relationship.label as string) || '',
    type: isRelationshipType(data.relationshipType)
      ? data.relationshipType
      : 'one-to-many',
    isNonIdentifying:
      typeof data.isNonIdentifying === 'boolean'
        ? data.isNonIdentifying
        : false,
    controlPointX:
      typeof data.controlPointX === 'number' ? data.controlPointX : undefined,
    controlPointY:
      typeof data.controlPointY === 'number' ? data.controlPointY : undefined,
  };
};

const createConfig = (data: RelationshipData): RelationshipConfig => ({
  type: data.type,
  isNonIdentifying: data.isNonIdentifying,
  controlPointX: data.controlPointX,
  controlPointY: data.controlPointY,
});

export const RelationshipEditor = ({
  selectedRelationship,
  relationships,
  onRelationshipChange,
  onRelationshipNameChange,
  onRelationshipDelete,
  onClose,
}: EdgeSelectorProps) => {
  const relationship = relationships.find((r) => r.id === selectedRelationship);
  const originalData = extractRelationshipData(relationship);

  const [localName, setLocalName] = useState(originalData.name);
  const [localConfig, setLocalConfig] = useState<RelationshipConfig>(
    createConfig(originalData),
  );

  useEffect(() => {
    const newData = extractRelationshipData(relationship);
    setLocalName(newData.name);
    setLocalConfig(createConfig(newData));
  }, [selectedRelationship, relationship]);

  const hasChanges =
    localName !== originalData.name ||
    localConfig.type !== originalData.type ||
    localConfig.isNonIdentifying !== originalData.isNonIdentifying;

  const handleSave = () => {
    if (localName !== originalData.name) {
      onRelationshipNameChange(selectedRelationship, localName);
    }
    if (
      localConfig.type !== originalData.type ||
      localConfig.isNonIdentifying !== originalData.isNonIdentifying
    ) {
      onRelationshipChange(selectedRelationship, localConfig);
    }
    onClose();
  };

  const handleDelete = () => {
    onRelationshipDelete(selectedRelationship);
  };

  return (
    <div className="absolute top-4 left-4 z-10 bg-schemafy-bg p-3 rounded-lg shadow-md border border-schemafy-light-gray space-y-3">
      <div className="text-sm font-medium text-schemafy-text">
        Edit Relationship
      </div>

      <div className="space-y-2">
        <label className="text-xs text-schemafy-text">Name</label>
        <input
          type="text"
          value={localName}
          onChange={(e) => setLocalName(e.target.value)}
          className="w-full px-2 py-1 text-sm bg-schemafy-secondary border border-schemafy-light-gray rounded text-schemafy-text focus:outline-none focus:ring-1 focus:ring-schemafy-text"
          placeholder="Relationship name"
        />
      </div>

      <RelationshipSelector config={localConfig} onChange={setLocalConfig} />

      <div className="flex gap-2">
        <Button onClick={handleDelete} variant="secondary" fullWidth>
          Delete
        </Button>
        <Button onClick={handleSave} disabled={!hasChanges} fullWidth>
          Save
        </Button>
        <Button onClick={onClose} variant="secondary" fullWidth>
          Close
        </Button>
      </div>
    </div>
  );
};
