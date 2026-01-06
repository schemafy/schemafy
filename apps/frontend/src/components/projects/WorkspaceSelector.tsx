import { ChevronDown } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
} from '@/components';
import type { Workspace } from '@/lib/api';

interface WorkspaceSelectorProps {
  workspaces: Workspace[];
  selectedWorkspace: Workspace | null;
  onSelect: (workspace: Workspace) => void;
}

export const WorkspaceSelector = ({
  workspaces,
  selectedWorkspace,
  onSelect,
}: WorkspaceSelectorProps) => {
  return (
    <div className="my-5">
      <DropdownMenu>
        <DropdownMenuTrigger className="flex items-center gap-2 px-4 py-2 bg-schemafy-secondary rounded-xl font-body-md text-schemafy-text hover:bg-schemafy-light-gray transition-colors cursor-pointer">
          {selectedWorkspace?.name || 'Select Workspace'}
          <ChevronDown size={16} />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start" className="min-w-[200px]">
          {workspaces.map((workspace) => (
            <button
              key={workspace.id}
              onClick={() => onSelect(workspace)}
              className={`w-full text-left px-3 py-2 rounded-lg font-body-sm transition-colors ${
                selectedWorkspace?.id === workspace.id
                  ? 'bg-schemafy-button-bg text-schemafy-button-text'
                  : 'text-schemafy-text hover:bg-schemafy-secondary'
              }`}
            >
              {workspace.name}
            </button>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
};
