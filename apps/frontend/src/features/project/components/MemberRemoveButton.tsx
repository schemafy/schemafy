import { MoreHorizontal } from 'lucide-react';
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components';
import type { ProjectMemberResponse } from '@/features/project/api';

interface MemberRemoveButtonProps {
  member: ProjectMemberResponse;
  onRemoveClick: (member: ProjectMemberResponse) => void;
}

export const MemberRemoveButton = ({
  member,
  onRemoveClick,
}: MemberRemoveButtonProps) => {
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
          className="schemafy-menu-button whitespace-nowrap px-3 py-2 text-left font-caption-md text-schemafy-destructive"
          onClick={() => onRemoveClick(member)}
        >
          Remove
        </Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
