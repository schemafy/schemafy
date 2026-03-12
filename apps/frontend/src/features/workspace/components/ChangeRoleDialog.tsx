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

interface ChangeRoleDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  selectedRole: string;
  onSelectedRoleChange: (role: string) => void;
  availableRoles: string[];
  onSave: () => void;
  isPending: boolean;
}

export const ChangeRoleDialog = ({
                                   open,
                                   onOpenChange,
                                   selectedRole,
                                   onSelectedRoleChange,
                                   availableRoles,
                                   onSave,
                                   isPending,
                                 }: ChangeRoleDialogProps) => {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Change Role</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-1.5 py-2">
          <label className="font-overline-xs text-schemafy-dark-gray">Role</label>
          <Select value={selectedRole} onValueChange={onSelectedRoleChange}>
            <SelectTrigger className="px-4 py-3 rounded-[12px] font-body-sm">
              <SelectValue/>
            </SelectTrigger>
            <SelectContent>
              {availableRoles.map((r) => (
                <SelectItem key={r} value={r}>
                  {r}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <DialogFooter>
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button size="sm" onClick={onSave} disabled={isPending}>
            Save
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};