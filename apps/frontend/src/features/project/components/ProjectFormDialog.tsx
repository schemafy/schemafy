import { EntityFormDialog } from '@/components';
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

  const handleSubmit = (data: { name: string; description: string }) => {
    if (mode === 'create') {
      createProject(data, { onSuccess: () => onOpenChange(false) });
    } else {
      updateProject(projectId, data, { onSuccess: () => onOpenChange(false) });
    }
  };

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={mode === 'create' ? 'Create Project' : 'Edit Project'}
      submitLabel={mode === 'create' ? 'Create' : 'Save'}
      initialName={initialName}
      initialDescription={initialDescription}
      isPending={isCreatingProject || isUpdatingProject}
      onSubmit={handleSubmit}
      namePlaceholder="Project name"
    />
  );
};
