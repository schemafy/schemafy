import { RelationshipSelector } from './RelationshipSelector';
import type { RelationshipType } from '../types';
import { Button } from '@/components';

export const Toolbar = ({
  onAddTable,
  currentRelationshipType,
  onRelationshipTypeChange,
}: {
  onAddTable: () => void;
  currentRelationshipType: RelationshipType;
  onRelationshipTypeChange: (type: RelationshipType) => void;
}) => (
  <div className="flex items-center gap-4 p-4 absolute bottom-0 z-100">
    <Button onClick={onAddTable}>Add Table</Button>
    <div className="flex items-center gap-2">
      <RelationshipSelector
        onSelect={onRelationshipTypeChange}
        currentType={currentRelationshipType}
      />
    </div>
  </div>
);
