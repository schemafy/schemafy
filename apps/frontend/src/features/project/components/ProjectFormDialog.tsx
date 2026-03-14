import { useEffect, useState } from 'react';
import { Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, } from '@/components';
import { useCreateProject, useUpdateProject } from '../hooks/useProjects';

interface ProjectFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mode: 'create' | 'edit';
  workspaceId: string;
  projectId?: string;
  initialName?: string;
  initialDescription?: string;
}

export const ProjectFormDialog = ({
                                    open,
                                    onOpenChange,
                                    mode,
                                    workspaceId,
                                    projectId = '',
                                    initialName = '',
                                    initialDescription = '',
                                  }: ProjectFormDialogProps) => {
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription);

  const {mutate: createProject, isPending: isCreating} =
    useCreateProject(workspaceId);
  const {mutate: updateProject, isPending: isUpdating} =
    useUpdateProject(projectId, workspaceId);

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
      createProject(data, {onSuccess: () => onOpenChange(false)});
    } else {
      updateProject(data, {onSuccess: () => onOpenChange(false)});
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {mode === 'create' ? 'Create Project' : 'Edit Project'}
          </DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4 py-2">
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">
              Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Project name"
              className="w-full px-4 py-3 border border-schemafy-light-gray rounded-[12px] font-body-sm placeholder-schemafy-dark-gray bg-schemafy-bg focus:outline-none"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">
              Description
            </label>
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
          <Button
            variant="outline"
            size="sm"
            onClick={() => onOpenChange(false)}
          >
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
