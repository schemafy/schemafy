import { EntityFormDialog } from '@/components';
import { useWorkspaces } from '../hooks/useWorkspaces';

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
  const {
    createWorkspace,
    updateWorkspace,
    isCreatingWorkspace,
    isUpdatingWorkspace,
  } = useWorkspaces();

  const handleSubmit = (data: { name: string; description: string }) => {
    if (mode === 'create') {
      createWorkspace(data, { onSuccess: () => onOpenChange(false) });
    } else {
      updateWorkspace(workspaceId, data, {
        onSuccess: () => onOpenChange(false),
      });
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
      isPending={isCreatingWorkspace || isUpdatingWorkspace}
      onSubmit={handleSubmit}
      namePlaceholder="Workspace name"
    />
  );
};
