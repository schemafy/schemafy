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
import type { Project, Workspace } from '@/lib/api';

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

  const { projects, totalPages, createProject, updateProject, deleteProject } =
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
    const payload = { name: data.name, description: data.description };

    const actions = {
      workspace: {
        create: () => createWorkspace(payload),
        edit: () => data.id && updateWorkspace(data.id, payload),
      },
      project: {
        create: () =>
          currentWorkspace && createProject(currentWorkspace.id, payload),
        edit: () =>
          currentWorkspace &&
          data.id &&
          updateProject(currentWorkspace.id, data.id, payload),
      },
    };

    await actions[entityType][mode]?.();
    closeDialog();
  };

  const handleEditWorkspace = (workspaceId: string, workspace: Workspace) => {
    openDialog('workspace', 'edit', {
      id: workspaceId,
      name: workspace.name,
      description: workspace.description,
    });
  };

  const handleEditProject = (projectId: string, project: Project) => {
    openDialog('project', 'edit', {
      id: projectId,
      name: project.name,
      description: project.description,
    });
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
            handleEditWorkspace(workspace.id, workspace);
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
          onEdit={handleEditProject}
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
