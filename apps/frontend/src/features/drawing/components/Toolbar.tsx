import { type ComponentType, useState } from 'react';
import { RelationshipSelector } from './RelationshipSelector';
import { SearchEntitiesDialog } from './SearchEntitiesDialog';
import type { RelationshipConfig } from '../types';
import { Button, Tooltip, TooltipContent, TooltipTrigger } from '@/components';
import {
  Hand,
  MessageCircleMore,
  MousePointer2,
  Search,
  Spline,
  Table,
} from 'lucide-react';

interface ToolbarProps {
  setActiveTool: (toolId: string) => void;
  activeTool: string;
  relationshipConfig: RelationshipConfig;
  onRelationshipConfigChange: (config: RelationshipConfig) => void;
}

const TOOLS = [
  { id: 'pointer', name: 'Pointer', shortcut: 'p', icon: MousePointer2 },
  { id: 'hand', name: 'Hand', shortcut: 'h', icon: Hand },
  {
    id: 'table',
    name: 'Add Entity',
    shortcut: 'e',
    icon: Table,
    action: 'addTable',
  },
  { id: 'memo', name: 'Add Memo', shortcut: 'm', icon: MessageCircleMore },
  {
    id: 'relationship',
    name: 'Change Relationship',
    icon: Spline,
    isRelationship: true,
  },
  { id: 'search', name: 'Search', icon: Search },
];

export const Toolbar = ({
  setActiveTool,
  activeTool,
  relationshipConfig,
  onRelationshipConfigChange,
}: ToolbarProps) => {
  const [isSearchDialogOpen, setIsSearchDialogOpen] = useState(false);

  const handleToolClick = (toolId: string) => {
    if (toolId === 'search') {
      setIsSearchDialogOpen(true);
      return;
    }
    if (activeTool === toolId) {
      setActiveTool('pointer');
    } else {
      setActiveTool(toolId);
    }
  };

  return (
    <>
      <div
        className="flex items-center gap-3 py-2 px-6 absolute bottom-4 left-1/2 -translate-x-1/2 z-20 bg-schemafy-bg rounded-lg"
        style={{
          boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
        }}
      >
        {TOOLS.map((tool) => (
          <Tool
            key={tool.id}
            id={tool.id}
            onClick={() => handleToolClick(tool.id)}
            Icon={tool.icon}
            name={tool.name}
            shortcut={tool.shortcut}
            isActive={activeTool === tool.id}
          />
        ))}

        {activeTool &&
          TOOLS.find((t) => t.id === activeTool)?.isRelationship && (
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2">
              <RelationshipSelector
                config={relationshipConfig}
                onChange={onRelationshipConfigChange}
              />
            </div>
          )}
      </div>

      <SearchEntitiesDialog
        open={isSearchDialogOpen}
        onOpenChange={setIsSearchDialogOpen}
      />
    </>
  );
};

const Tool = ({
  onClick,
  Icon,
  id,
  name,
  shortcut,
  isActive,
}: {
  onClick: () => void;
  Icon: ComponentType<{ size: number; color: string }>;
  id: string;
  name: string;
  shortcut?: string;
  isActive: boolean;
}) => {
  const color = isActive
    ? 'var(--color-schemafy-text)'
    : 'var(--color-schemafy-tools)';

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          onClick={onClick}
          variant={'none'}
          size={'none'}
          data-testid={`toolbar-${id}`}
          className={`
        p-2 rounded-md transition-colors duration-200
        hover:bg-schemafy-secondary
      `}
        >
          <Icon size={16} color={color} />
        </Button>
      </TooltipTrigger>
      <TooltipContent>
        <div className="flex flex-col items-center gap-0.5">
          <span>{name}</span>
          {shortcut && <span className="text-xs opacity-60">{shortcut}</span>}
        </div>
      </TooltipContent>
    </Tooltip>
  );
};
