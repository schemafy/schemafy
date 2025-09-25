import { RelationshipSelector } from './RelationshipSelector';
import type { RelationshipConfig } from '../types';
import { Button } from '@/components';

interface ToolbarProps {
  onAddTable: () => void;
  relationshipConfig: RelationshipConfig;
  onRelationshipConfigChange: (config: RelationshipConfig) => void;
}

export const Toolbar = ({
  onAddTable,
  relationshipConfig,
  onRelationshipConfigChange,
}: ToolbarProps) => {
  return (
    <div className="flex items-center gap-4 p-4 absolute bottom-0 z-100">
      <Button onClick={onAddTable}>Add Table</Button>
      <div className="flex items-center gap-2">
        <RelationshipSelector
          config={relationshipConfig}
          onChange={onRelationshipConfigChange}
        />
      </div>
    </div>
  );
};
