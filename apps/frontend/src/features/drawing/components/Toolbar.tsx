import { memo, type ComponentType, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { RelationshipSelector } from './RelationshipSelector';
import { SearchEntitiesDialog } from './SearchEntitiesDialog';
import type { RelationshipConfig } from '../types';
import { useUndoRedo } from '../hooks/useUndoRedo';
import { Button, Tooltip, TooltipContent, TooltipTrigger } from '@/components';
import {
  Hand,
  MessageCircleMore,
  MousePointer2,
  Redo2,
  Search,
  Spline,
  Table,
  Undo2,
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

export const Toolbar = memo(
  observer(
    ({
      setActiveTool,
      activeTool,
      relationshipConfig,
      onRelationshipConfigChange,
    }: ToolbarProps) => {
      const [isSearchDialogOpen, setIsSearchDialogOpen] = useState(false);
      const { handleUndo, handleRedo, canUndo, canRedo } = useUndoRedo();

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
          <div className="schemafy-canvas-panel absolute bottom-4 left-1/2 z-20 flex h-12 -translate-x-1/2 items-center gap-1 rounded-2xl px-2 sm:bottom-6 sm:gap-1">
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

            <div className="mx-1 h-5 w-px bg-schemafy-glass-border" />

            <UndoRedoButton
              icon={Undo2}
              name="Undo"
              disabled={!canUndo}
              onClick={handleUndo}
            />
            <UndoRedoButton
              icon={Redo2}
              name="Redo"
              disabled={!canRedo}
              onClick={handleRedo}
            />

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
    },
  ),
);

const UndoRedoButton = ({
  onClick,
  icon: Icon,
  name,
  disabled,
}: {
  onClick: () => void;
  icon: ComponentType<{ size: number; color: string }>;
  name: string;
  disabled: boolean;
}) => {
  const color = disabled
    ? 'var(--color-schemafy-tools)'
    : 'var(--color-schemafy-soft-blue)';

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          onClick={onClick}
          disabled={disabled}
          variant={'none'}
          size={'none'}
          className={`
        h-9 w-9 rounded-xl transition-all duration-200
        hover:bg-schemafy-secondary
        ${disabled ? 'cursor-not-allowed opacity-30' : ''}
      `}
        >
          <Icon size={16} color={color} />
        </Button>
      </TooltipTrigger>
      <TooltipContent>
        <span>{name}</span>
      </TooltipContent>
    </Tooltip>
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
    ? 'var(--color-schemafy-soft-blue)'
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
        h-9 w-9 rounded-xl transition-all duration-200
        hover:bg-schemafy-secondary
        ${isActive ? 'bg-schemafy-soft-blue/10 ring-1 ring-schemafy-soft-blue/30' : ''}
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
