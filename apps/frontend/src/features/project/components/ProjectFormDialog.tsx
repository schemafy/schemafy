import { EntityFormDialog } from '@/components';
import { useProjects } from '../hooks/useProjects';
import { CreateProjectDialog } from './CreateProjectDialog';

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
