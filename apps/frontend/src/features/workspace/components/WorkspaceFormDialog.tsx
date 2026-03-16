import { EntityFormDialog } from '@/components';
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
  const { mutate: createWorkspace, isPending: isCreating } = useCreateWorkspace();
  const { mutate: updateWorkspace, isPending: isUpdating } = useUpdateWorkspace(workspaceId);

  const handleSubmit = (data: { name: string; description: string }) => {
    if (mode === 'create') {
      createWorkspace(data, { onSuccess: () => onOpenChange(false) });
    } else {
      updateWorkspace(data, { onSuccess: () => onOpenChange(false) });
    }
  };

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={mode === 'create' ? 'Create Workspace' : 'Edit Workspace'}
      submitLabel={mode === 'create' ? 'Create' : 'Save'}
      initialName={initialName}
      initialDescription={initialDescription}
      isPending={isCreating || isUpdating}
      onSubmit={handleSubmit}
      namePlaceholder="Workspace name"
    />
  );
};
