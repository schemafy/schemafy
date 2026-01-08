import { useState } from 'react';
import {
  Button,
  Pagination,
  WorkspaceSelector,
  ProjectSearchBar,
  ProjectTable,
  EntityFormDialog,
} from '@/components';
import type { EntityFormData } from '@/components';
import { useProjectStore, useWorkspaceStore } from '@/hooks';
import { observer } from 'mobx-react-lite';

type DialogState = {
  open: boolean;
  entityType: 'workspace' | 'project';
  mode: 'create' | 'edit';
  initialData?: EntityFormData;
};

export const ProjectsPage = observer(() => {
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [dialogState, setDialogState] = useState<DialogState>({
    open: false,
    entityType: 'workspace',
    mode: 'create',
  });

  const {
    workspaces,
    currentWorkspace,
    setWorkspace,
    createWorkspace,
    updateWorkspace,
    deleteWorkspace,
  } = useWorkspaceStore();

  const { projects, totalPages, createProject, deleteProject } =
    useProjectStore();

  const filteredProjects = projects.filter((project) =>
    project.name.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const openDialog = (
    entityType: 'workspace' | 'project',
    mode: 'create' | 'edit',
    initialData?: EntityFormData,
  ) => {
    setDialogState({ open: true, entityType, mode, initialData });
  };

  const closeDialog = () => {
    setDialogState((prev) => ({ ...prev, open: false }));
  };

  const handleDialogSubmit = async (data: EntityFormData) => {
    const { entityType, mode } = dialogState;

    if (entityType === 'workspace') {
      if (mode === 'create') {
        await createWorkspace(data);
      } else if (mode === 'edit' && data.id) {
        await updateWorkspace(data.id, {
          name: data.name,
          description: data.description,
        });
      }
    } else if (entityType === 'project') {
      if (!currentWorkspace) return;
      if (mode === 'create') {
        await createProject(currentWorkspace.id, data);
      }
    }

    closeDialog();
  };

  const handleDeleteProject = (workspaceId: string, projectId: string) => {
    deleteProject(workspaceId, projectId);
  };

  return (
    <div className="min-h-screen bg-schemafy-bg w-full">
      <div className="flex flex-col">
        <WorkspaceSelector
          workspaces={workspaces}
          selectedWorkspace={currentWorkspace}
          onSelect={setWorkspace}
          onEdit={(workspace) => {
            openDialog('workspace', 'edit', {
              id: workspace.id,
              name: workspace.name,
              description: workspace.description,
            });
          }}
          onDelete={deleteWorkspace}
        />

        <ProjectSearchBar value={searchQuery} onChange={setSearchQuery} />

        <div className="flex items-center justify-between mb-6">
          <h2 className="font-heading-md text-schemafy-text">Projects</h2>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => openDialog('workspace', 'create')}
            >
              New Workspace
            </Button>
            <Button onClick={() => openDialog('project', 'create')}>
              New Project
            </Button>
          </div>
        </div>

        <ProjectTable
          projects={filteredProjects}
          onDelete={handleDeleteProject}
        />

        <div className="flex justify-center">
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
          />
        </div>
      </div>

      <EntityFormDialog
        open={dialogState.open}
        onOpenChange={(open) => !open && closeDialog()}
        onSubmit={handleDialogSubmit}
        entityType={dialogState.entityType}
        mode={dialogState.mode}
        initialData={dialogState.initialData}
      />
    </div>
  );
});
