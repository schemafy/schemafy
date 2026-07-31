import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import { cn } from '@/lib';

export type WorkspaceItem = {
  id: string;
  name: string;
  description?: string;
};

interface WorkspaceSidebarProps {
  workspaces: WorkspaceItem[];
  pinnedItem?: WorkspaceItem;
  selectedId: string;
  onSelect: (id: string) => void;
  onAdd?: () => void;
  isOpen: boolean;
  onToggle: () => void;
  hasNextPage?: boolean;
  isFetchingNextPage?: boolean;
  onLoadMore?: () => void;
}

export const WorkspaceSidebar = ({
  workspaces,
  pinnedItem,
  selectedId,
  onSelect,
  onAdd,
  isOpen,
  onToggle,
  hasNextPage = false,
  isFetchingNextPage = false,
  onLoadMore,
}: WorkspaceSidebarProps) => {
  return (
    <aside
      className={cn(
        'relative z-10 flex shrink-0 flex-col overflow-hidden border-b shrink-0 border-schemafy-glass-border bg-schemafy-bg transition-[width] duration-300 md:min-h-full md:border-b-0 md:border-r',
        isOpen ? 'w-full md:w-72' : 'w-full md:w-16',
      )}
    >
      <div
        className={cn(
          'flex items-center border-b border-schemafy-glass-border/70 px-4 py-4 shrink-0 transition-all duration-300',
          isOpen ? 'justify-between' : 'justify-center',
        )}
      >
        {isOpen && (
          <div className="min-w-0">
            <p className="truncate font-heading-xs text-schemafy-text">
              Workspaces
            </p>
          </div>
        )}
        <div className={cn('flex items-center gap-1', isOpen && 'gap-2')}>
          {isOpen && (
            <button
              type="button"
              onClick={onAdd}
              className="schemafy-icon-button schemafy-focus-ring flex h-8 w-8 items-center justify-center"
              aria-label="Add workspace"
            >
              <Plus size={14} />
            </button>
          )}
          <button
            type="button"
            onClick={onToggle}
            className="schemafy-icon-button schemafy-focus-ring flex h-8 w-8 items-center justify-center"
            aria-label={
              isOpen ? 'Collapse workspace sidebar' : 'Open workspace sidebar'
            }
          >
            {isOpen ? <ChevronLeft size={16} /> : <ChevronRight size={16} />}
          </button>
        </div>
      </div>

      <ul
        className={cn(
          'schemafy-scrollbar flex flex-col gap-1 overflow-y-auto px-3 py-3 transition-opacity duration-200 min-h-0 flex-1',
          isOpen
            ? 'opacity-100'
            : 'pointer-events-none h-0 opacity-0 md:h-auto',
        )}
      >
        {pinnedItem && (
          <li>
            <button
              onClick={() => onSelect(pinnedItem.id)}
              className={cn(
                'w-full text-left px-3 py-2.5 rounded-[10px] transition-colors',
                selectedId === pinnedItem.id
                  ? 'bg-schemafy-secondary'
                  : 'hover:bg-schemafy-secondary',
              )}
            >
              <p className="font-overline-sm text-schemafy-text">
                {pinnedItem.name}
              </p>
              {pinnedItem.description && (
                <p className="font-caption-sm text-schemafy-dark-gray mt-0.5 truncate">
                  {pinnedItem.description}
                </p>
              )}
            </button>
          </li>
        )}
        {workspaces.map((workspace) => (
          <li key={workspace.id}>
            <button
              onClick={() => onSelect(workspace.id)}
              className={cn(
                'schemafy-focus-ring group w-full rounded-xl px-3 py-2.5 text-left transition-colors duration-200',
                selectedId === workspace.id
                  ? 'bg-schemafy-secondary text-schemafy-text'
                  : 'text-schemafy-dark-gray hover:bg-schemafy-secondary/70 hover:text-schemafy-text',
              )}
            >
              <div className="flex items-center gap-3">
                <span
                  className={cn(
                    'h-2.5 w-2.5 shrink-0 rounded-full border border-schemafy-glass-border',
                    selectedId === workspace.id
                      ? 'bg-schemafy-soft-blue'
                      : 'bg-schemafy-dark-gray/30 group-hover:bg-schemafy-dark-gray/50',
                  )}
                />
                <p className="min-w-0 truncate font-overline-sm text-schemafy-text">
                  {workspace.name}
                </p>
              </div>
              {workspace.description && (
                <p className="mt-1.5 truncate pl-5 font-caption-sm text-schemafy-dark-gray">
                  {workspace.description}
                </p>
              )}
            </button>
          </li>
        ))}
      </ul>

      {hasNextPage && onLoadMore && (
        <div
          className={cn(
            'shrink-0 px-3 py-2 transition-opacity duration-200',
            isOpen ? 'opacity-100' : 'opacity-0 pointer-events-none',
          )}
        >
          <button
            onClick={() => onLoadMore()}
            disabled={isFetchingNextPage}
            className="w-full text-left px-3 py-2 rounded-[10px] transition-colors hover:bg-schemafy-secondary disabled:opacity-50 disabled:pointer-events-none"
          >
            <span className="font-overline-xs text-schemafy-dark-gray">
              {isFetchingNextPage ? 'Loading...' : 'Show More'}
            </span>
          </button>
        </div>
      )}
    </aside>
  );
};
