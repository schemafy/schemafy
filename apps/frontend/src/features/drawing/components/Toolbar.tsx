import { type ComponentType, useState } from 'react';
import { RelationshipSelector } from './RelationshipSelector';
import type { RelationshipConfig } from '../types';
import {
  Button,
  Tooltip,
  TooltipTrigger,
  TooltipContent,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  ListItem,
} from '@/components';
import {
  Search,
  Table,
  MessageCircleMore,
  Spline,
  MousePointer2,
  Hand,
} from 'lucide-react';

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
    id: 'memo',
    name: 'Add Memo',
    label: 'Memo',
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
            onClick={() => handleToolClick(tool.id)}
            Icon={tool.icon}
            name={tool.name}
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

      <Dialog open={isSearchDialogOpen} onOpenChange={setIsSearchDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <div className="flex gap-2.5 items-center">
              <Table size={16} color="var(--color-schemafy-dark-gray)" />
              <DialogTitle>Entities</DialogTitle>
            </div>
          </DialogHeader>
          <div className="py-2 px-3 flex justify-between items-center bg-schemafy-secondary rounded-[10px]">
            <input
              type="text"
              placeholder="Search for entities..."
              className="w-full focus:border-none outline-none focus:outline-none placeholder:text-schemafy-dark-gray border-none text-schemafy-text font-body-xs"
            />
            <Search size={16} color="var(--color-schemafy-dark-gray)" />
          </div>
          <ul className="flex flex-col w-full max-h-[12.5rem] gap-2.5 overflow-y-scroll overflow-x-hidden pr-2 [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-schemafy-light-gray [&::-webkit-scrollbar-track]:bg-transparent">
            <ListItem
              count={6}
              name="User"
              description={'사용자 정보'}
              date={new Date()}
            />
            <ListItem
              count={6}
              name="User"
              description={'사용자 정보'}
              date={new Date()}
            />
            <ListItem
              count={6}
              name="User"
              description={'사용자 정보'}
              date={new Date()}
            />
            <ListItem
              count={6}
              name="User"
              description={'사용자 정보'}
              date={new Date()}
            />
          </ul>
        </DialogContent>
      </Dialog>
    </>
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
