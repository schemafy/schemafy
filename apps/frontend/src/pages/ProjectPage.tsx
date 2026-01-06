import { useState } from 'react';
import {
  Button,
  Pagination,
  WorkspaceSelector,
  ProjectSearchBar,
  ProjectTable,
  CreateWorkspaceDialog,
  CreateProjectDialog,
} from '@/components';
import { useProjectStore } from '@/hooks';
import { observer } from 'mobx-react-lite';

export const ProjectsPage = observer(() => {
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isWorkspaceDialogOpen, setIsWorkspaceDialogOpen] = useState(false);
  const [isProjectDialogOpen, setIsProjectDialogOpen] = useState(false);

  const {
    workspaces,
    currentWorkspace,
    projects,
    totalPages,
    setWorkspace,
    createWorkspace,
    createProject,
    deleteProject,
  } = useProjectStore();

  const filteredProjects = projects.filter((project) =>
    project.name.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const handleCreateWorkspace = async (data: {
    name: string;
    description: string;
  }) => {
    await createWorkspace(data);
    setIsWorkspaceDialogOpen(false);
  };

  const handleCreateProject = async (data: {
    name: string;
    description: string;
  }) => {
    if (!currentWorkspace) return;
    await createProject(currentWorkspace.id, data);
    setIsProjectDialogOpen(false);
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
        />

        <ProjectSearchBar value={searchQuery} onChange={setSearchQuery} />

        <div className="flex items-center justify-between mb-6">
          <h2 className="font-heading-md text-schemafy-text">Projects</h2>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => setIsWorkspaceDialogOpen(true)}
            >
              New Workspace
            </Button>
            <Button onClick={() => setIsProjectDialogOpen(true)}>
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

      <CreateWorkspaceDialog
        open={isWorkspaceDialogOpen}
        onOpenChange={setIsWorkspaceDialogOpen}
        onSubmit={handleCreateWorkspace}
      />

      <CreateProjectDialog
        open={isProjectDialogOpen}
        onOpenChange={setIsProjectDialogOpen}
        onSubmit={handleCreateProject}
      />
    </div>
  );
});
