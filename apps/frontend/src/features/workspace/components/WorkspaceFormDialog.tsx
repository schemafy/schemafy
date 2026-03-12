import { useEffect, useState } from 'react';
import { Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, } from '@/components';
import { useCreateWorkspace, useUpdateWorkspace } from '../hooks/useWorkspaces';

interface WorkspaceFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mode: 'create' | 'edit';
  workspaceId?: string;
  initialName?: string;
  initialDescription?: string;
}

export const WorkspaceFormDialog = ({
                                      open,
                                      onOpenChange,
                                      mode,
                                      workspaceId = '',
                                      initialName = '',
                                      initialDescription = '',
                                    }: WorkspaceFormDialogProps) => {
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription);

  const {mutate: createWorkspace, isPending: isCreating} = useCreateWorkspace();
  const {mutate: updateWorkspace, isPending: isUpdating} = useUpdateWorkspace(workspaceId);

  useEffect(() => {
    if (open) {
      setName(initialName);
      setDescription(initialDescription);
    }
  }, [open, initialName, initialDescription]);

  const handleSubmit = () => {
    if (!name.trim()) return;
    const data = {name, description: description || ''};
    if (mode === 'create') {
      createWorkspace(data, {onSuccess: () => onOpenChange(false)});
    } else {
      updateWorkspace(data, {onSuccess: () => onOpenChange(false)});
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {mode === 'create' ? 'Create Workspace' : 'Edit Workspace'}
          </DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4 py-2">
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Workspace name"
              className="w-full px-4 py-3 border border-schemafy-light-gray rounded-[12px] font-body-sm placeholder-schemafy-dark-gray bg-schemafy-bg focus:outline-none"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">Description</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional description"
              className="w-full px-4 py-3 border border-schemafy-light-gray rounded-[12px] font-body-sm placeholder-schemafy-dark-gray bg-schemafy-bg focus:outline-none"
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            size="sm"
            onClick={handleSubmit}
            disabled={!name.trim() || isCreating || isUpdating}
          >
            {mode === 'create' ? 'Create' : 'Save'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
