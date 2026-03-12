import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import { cn } from '@/lib';

export type WorkspaceItem = {
  id: string;
  name: string;
  description?: string;
};

interface WorkspaceSidebarProps {
  workspaces: WorkspaceItem[];
  selectedId: string;
  onSelect: (id: string) => void;
  onAdd?: () => void;
  isOpen: boolean;
  onToggle: () => void;
}

export const WorkspaceSidebar = ({
  workspaces,
  selectedId,
  onSelect,
  onAdd,
  isOpen,
  onToggle,
}: WorkspaceSidebarProps) => {
  return (
    <aside
      className={cn(
        'min-h-full border-r border-schemafy-light-gray flex flex-col shrink-0 relative z-10 bg-schemafy-bg overflow-hidden transition-[width] duration-300',
        isOpen ? 'w-64' : 'w-12',
      )}
    >
      <div
        className={cn(
          'flex items-center py-5 px-3 transition-all duration-300',
          isOpen ? 'justify-between' : 'justify-center',
        )}
      >
        {isOpen && (
          <span className="font-caption-sm text-schemafy-dark-gray tracking-widest uppercase whitespace-nowrap">
            Workspaces
          </span>
        )}
        <div className={cn('flex items-center gap-1', isOpen && 'gap-2')}>
          {isOpen && (
            <button
              onClick={onAdd}
              className="text-schemafy-dark-gray hover:text-schemafy-text transition-colors"
            >
              <Plus size={14} />
            </button>
          )}
          <button
            onClick={onToggle}
            className="text-schemafy-dark-gray hover:text-schemafy-text transition-colors"
          >
            {isOpen ? <ChevronLeft size={16} /> : <ChevronRight size={16} />}
          </button>
        </div>
      </div>

      <ul
        className={cn(
          'flex flex-col gap-0.5 px-3 transition-opacity duration-200',
          isOpen ? 'opacity-100' : 'opacity-0 pointer-events-none',
        )}
      >
        {workspaces.map((workspace) => (
          <li key={workspace.id}>
            <button
              onClick={() => onSelect(workspace.id)}
              className={cn(
                'w-full text-left px-3 py-2.5 rounded-[10px] transition-colors',
                selectedId === workspace.id
                  ? 'bg-schemafy-secondary'
                  : 'hover:bg-schemafy-secondary',
              )}
            >
              <p className="font-overline-sm text-schemafy-text">
                {workspace.name}
              </p>
              {workspace.description && (
                <p className="font-caption-sm text-schemafy-dark-gray mt-0.5 truncate">
                  {workspace.description}
                </p>
              )}
            </button>
          </li>
        ))}
      </ul>
    </aside>
  );
};
