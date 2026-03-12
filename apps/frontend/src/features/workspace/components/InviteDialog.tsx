import { useState } from 'react';
import {
  Button,
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components';
import { useCreateInvitation } from '../hooks/useWorkspaces';
import { availableRoles } from "@/features/workspace/utils/role";

interface InviteDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  workspaceId: string;
  currentUserRole: string;
}

export const InviteDialog = ({
                               open,
                               onOpenChange,
                               workspaceId,
                               currentUserRole,
                             }: InviteDialogProps) => {
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<string>('MEMBER');

  const {mutate: createInvitation, isPending} = useCreateInvitation(workspaceId);

  const roles = availableRoles(currentUserRole);

  const handleSubmit = () => {
    if (!email.trim()) return;
    createInvitation(
      {email, role},
      {
        onSuccess: () => {
          setEmail('');
          setRole('MEMBER');
          onOpenChange(false);
        },
      },
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Invite Member</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4 py-2">
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="member@example.com"
              className="w-full px-4 py-3 border border-schemafy-light-gray rounded-[12px] font-body-sm placeholder-schemafy-dark-gray bg-schemafy-bg focus:outline-none"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">Role</label>
            <Select value={role} onValueChange={setRole}>
              <SelectTrigger className="px-4 py-3 rounded-[12px] font-body-sm">
                <SelectValue/>
              </SelectTrigger>
              <SelectContent>
                {roles.map((r) => (
                  <SelectItem key={r} value={r}>
                    {r}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button size="sm" onClick={handleSubmit} disabled={!email.trim() || isPending}>
            Invite
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
