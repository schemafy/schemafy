import { MoreHorizontal } from 'lucide-react';
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components';
import type { ProjectSummaryResponse } from '@/features/project/api';

interface SharedProjectActionsProps {
  project: ProjectSummaryResponse;
  onLeaveClick: (project: ProjectSummaryResponse) => void;
}

export const SharedProjectActions = ({
  project,
  onLeaveClick,
}: SharedProjectActionsProps) => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="schemafy-icon-button schemafy-focus-ring flex h-8 w-8 items-center justify-center"
        >
          <MoreHorizontal size={16} />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        sideOffset={4}
        align="end"
        className="!min-w-0 flex flex-col gap-0.5 !p-1.5"
      >
        <Button
          variant="none"
          size="none"
          className="schemafy-menu-button whitespace-nowrap px-3 py-2 font-caption-md text-schemafy-destructive"
          onClick={() => onLeaveClick(project)}
        >
          Leave
        </Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
