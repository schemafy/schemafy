import {
  Button,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components';
import type { ProjectMemberResponse } from '@/features/project/api';

interface ConfirmMemberRemoveDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  member: ProjectMemberResponse | null;
  onConfirm: () => void;
}

export const ConfirmMemberRemoveDialog = ({
  open,
  onOpenChange,
  member,
  onConfirm,
}: ConfirmMemberRemoveDialogProps) => {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Remove Member</DialogTitle>
        </DialogHeader>
        <p className="py-2 font-body-sm text-schemafy-dark-gray">
          Would you like to remove {member?.userName}({member?.userEmail}) from
          the project?
        </p>
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end [&>*]:w-full sm:[&>*]:w-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onOpenChange(false)}
          >
            Cancel
          </Button>
          <Button
            variant="none"
            size="sm"
            className="bg-schemafy-destructive text-white"
            onClick={onConfirm}
          >
            Remove
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
