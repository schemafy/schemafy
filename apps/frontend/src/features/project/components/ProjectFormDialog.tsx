import { useEffect, useState } from 'react';
import {
  Button,
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  EntityFormDialog,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components';
import { useVendors } from '@/features/vendor';
import { useProjects } from '../hooks/useProjects';

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
  const { createProject, updateProject, isCreatingProject, isUpdatingProject } =
    useProjects(workspaceId);

  if (mode === 'create') {
    return (
      <CreateProjectDialog
        open={open}
        onOpenChange={onOpenChange}
        isPending={isCreatingProject}
        onSubmit={(data) =>
          createProject(data, { onSuccess: () => onOpenChange(false) })
        }
      />
    );
  }

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title="Edit Project"
      submitLabel="Save"
      initialName={initialName}
      initialDescription={initialDescription}
      isPending={isUpdatingProject}
      onSubmit={(data) =>
        updateProject(projectId, data, {
          onSuccess: () => onOpenChange(false),
        })
      }
      namePlaceholder="Project name"
    />
  );
};

interface CreateProjectDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isPending: boolean;
  onSubmit: (data: {
    dbVendorId: string;
    name: string;
    description: string;
  }) => void;
}

const CreateProjectDialog = ({
  open,
  onOpenChange,
  isPending,
  onSubmit,
}: CreateProjectDialogProps) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [dbVendorId, setDbVendorId] = useState('');
  const {
    data: vendors,
    isLoading: isVendorsLoading,
    isError: isVendorsError,
  } = useVendors();

  useEffect(() => {
    if (!open) return;
    setName('');
    setDescription('');
    setDbVendorId('');
  }, [open]);

  const handleSubmit = () => {
    if (!name.trim() || !dbVendorId) return;
    onSubmit({ dbVendorId, name, description: description || '' });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Create Project</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4 py-2">
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">
              Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Project name"
              className="schemafy-input w-full px-4 py-3 font-body-sm"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">
              Description
            </label>
            <input
              type="text"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Optional description"
              className="schemafy-input w-full px-4 py-3 font-body-sm"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">
              Database Profile
            </label>
            <Select
              value={dbVendorId}
              onValueChange={setDbVendorId}
              disabled={isVendorsLoading || isVendorsError}
            >
              <SelectTrigger className="px-4 py-3 font-body-sm">
                <SelectValue
                  placeholder={
                    isVendorsLoading
                      ? 'Loading profiles...'
                      : 'Select a profile'
                  }
                />
              </SelectTrigger>
              <SelectContent>
                {vendors?.map((vendor) => (
                  <SelectItem key={vendor.id} value={vendor.id}>
                    {vendor.displayName} ({vendor.name} {vendor.version})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {isVendorsError && (
              <p className="font-caption-md text-schemafy-destructive">
                Failed to load database profiles.
              </p>
            )}
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
            disabled={!name.trim() || !dbVendorId || isPending}
          >
            Create
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
