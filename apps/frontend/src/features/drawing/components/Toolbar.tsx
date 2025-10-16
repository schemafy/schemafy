import type { ComponentType } from 'react';
import { RelationshipSelector } from './RelationshipSelector';
import type { RelationshipConfig } from '../types';
import { Button, Tooltip, TooltipTrigger, TooltipContent } from '@/components';
import { Search, Table, MessageCircleMore, Spline, MousePointer2, Hand } from 'lucide-react';

interface ToolbarProps {
  setActiveTool: (toolId: string) => void;
  activeTool: string;
  relationshipConfig: RelationshipConfig;
  onRelationshipConfigChange: (config: RelationshipConfig) => void;
}

const TOOLS = [
  {
    id: 'pointer',
    name: 'Pointer',
    label: 'MousePointer',
    icon: MousePointer2,
  },
  {
    id: 'hand',
    name: 'Hand',
    label: 'Hand',
    icon: Hand,
  },
  {
    id: 'table',
    name: 'Add Entity',
    label: 'Table',
    icon: Table,
    action: 'addTable',
  },
  {
    id: 'relationship',
    name: 'Change Relationship',
    label: 'RelationshipSelector',
    icon: Spline,
    isRelationship: true,
  },
  {
    id: 'comment',
    name: 'Add Comment',
    label: 'Comment',
    icon: MessageCircleMore,
  },
  {
    id: 'search',
    name: 'Search Entities',
    label: 'Search',
    icon: Search,
  },
];

export const Toolbar = ({
  setActiveTool,
  activeTool,
  relationshipConfig,
  onRelationshipConfigChange,
}: ToolbarProps) => {
  const handleToolClick = (toolId: string) => {
    if (activeTool === toolId) {
      setActiveTool('pointer');
    } else {
      setActiveTool(toolId);
    }
  };

  return (
    <div
      className="flex items-center gap-3 py-2 px-6 absolute bottom-3 left-1/2 -translate-x-1/2 z-100 bg-schemafy-bg rounded-lg"
      style={{
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
      }}
    >
      {TOOLS.map((tool) => (
        <Tool
          key={tool.id}
          onClick={() => handleToolClick(tool.id)}
          Icon={tool.icon}
          name={tool.name}
          isActive={activeTool === tool.id}
        />
      ))}

      {activeTool && TOOLS.find((t) => t.id === activeTool)?.isRelationship && (
        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2">
          <RelationshipSelector config={relationshipConfig} onChange={onRelationshipConfigChange} />
        </div>
      )}
    </div>
  );
};

const Tool = ({
  onClick,
  Icon,
  name,
  isActive,
}: {
  onClick: () => void;
  Icon: ComponentType<{ size: number; color: string }>;
  name: string;
  isActive: boolean;
}) => {
  const color = isActive ? 'var(--color-schemafy-text)' : 'var(--color-schemafy-tools)';

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          onClick={onClick}
          variant={'none'}
          size={'none'}
          className={`
        p-2 rounded-md transition-colors duration-200 
        hover:bg-schemafy-secondary
      `}
        >
          <Icon size={16} color={color} />
        </Button>
      </TooltipTrigger>
      <TooltipContent>
        <p>{name}</p>
      </TooltipContent>
    </Tooltip>
  );
};
