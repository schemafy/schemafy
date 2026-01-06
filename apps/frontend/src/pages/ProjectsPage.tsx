import { useEffect, useState } from 'react';
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  Pagination,
  Menu,
  MenuItem,
  Button,
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components';
import { ChevronDown } from 'lucide-react';
import { ProjectStore } from '@/store';
import type { ProjectRequest, WorkspaceRequest } from '@/lib/api';
import { observer } from 'mobx-react-lite';

export const ProjectsPage = observer(() => {
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isWorkspaceDialogOpen, setIsWorkspaceDialogOpen] = useState(false);
  const [isProjectDialogOpen, setIsProjectDialogOpen] = useState(false);

  const [createWorkspaceName, setCreateWorkspaceName] = useState('');
  const [createWorkspaceDescription, setCreateWorkspaceDescription] =
    useState('');

  const [createProjectName, setCreateProjectName] = useState('');
  const [createProjectDescription, setCreateProjectDescription] = useState('');

  const projectStore = ProjectStore.getInstance();

  const selectedWorkspace = projectStore.currentWorkspace?.name || '';

  const filteredProjects = projectStore.projects?.content.filter((project) =>
    project.name.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const handleCreateWorkSpace = (workspace: WorkspaceRequest) => {
    projectStore.createWorkspace(workspace);
    setIsWorkspaceDialogOpen(false);
  };

  const handleCreateProject = (
    workspaceId: string,
    project: ProjectRequest,
  ) => {
    projectStore.createProject(workspaceId, project);
    setIsProjectDialogOpen(false);
  };

  const handleDeleteProject = (workspaceId: string, projectId: string) => {
    projectStore.deleteProject(workspaceId, projectId);
  };

  useEffect(() => {
    projectStore.fetchWorkspaces();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!projectStore.currentWorkspace) return;

    projectStore.fetchProjects(
      projectStore.currentWorkspace.id,
      currentPage - 1,
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectStore.currentWorkspace]);

  return (
    <div className="min-h-screen bg-schemafy-bg w-full">
      <div className="flex flex-col">
        <div className="my-5">
          <DropdownMenu>
            <DropdownMenuTrigger className="flex items-center gap-2 px-4 py-2 bg-schemafy-secondary rounded-xl font-body-md text-schemafy-text hover:bg-schemafy-light-gray transition-colors cursor-pointer">
              {selectedWorkspace}
              <ChevronDown size={16} />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="min-w-[200px]">
              {projectStore.workspaces?.content.map((workspace) => (
                <button
                  key={workspace.name}
                  onClick={() => {
                    projectStore.setWorkspace(workspace);
                  }}
                  className={`w-full text-left px-3 py-2 rounded-lg font-body-sm transition-colors ${
                    selectedWorkspace === workspace.name
                      ? 'bg-schemafy-button-bg text-schemafy-button-text'
                      : 'text-schemafy-text hover:bg-schemafy-secondary'
                  }`}
                >
                  {workspace.name}
                </button>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        <div className="mb-8">
          <div className="relative">
            <svg
              width="20"
              height="20"
              viewBox="0 0 20 20"
              fill="none"
              className="absolute left-4 top-1/2 -translate-y-1/2 text-schemafy-dark-gray"
            >
              <circle
                cx="9"
                cy="9"
                r="7"
                stroke="currentColor"
                strokeWidth="2"
              />
              <path
                d="M14 14L18 18"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
            </svg>
            <input
              type="text"
              placeholder="Search projects"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-12 pr-4 py-3 bg-schemafy-secondary rounded-xl font-body-md text-schemafy-text placeholder-schemafy-dark-gray focus:outline-none focus:ring-2 focus:ring-schemafy-button-bg"
            />
          </div>
        </div>

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

        <div className="bg-schemafy-bg border border-schemafy-light-gray rounded-lg mb-6">
          <Table>
            <TableHeader>
              <TableRow className="border-b border-schemafy-light-gray hover:bg-transparent">
                <TableHead className="font-overline-sm text-schemafy-text">
                  Name
                </TableHead>
                <TableHead className="font-overline-sm text-schemafy-text">
                  Last Modified
                </TableHead>
                <TableHead className="font-overline-sm text-schemafy-text">
                  Access
                </TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredProjects?.map((project) => (
                <TableRow key={project.id}>
                  <TableCell className="font-body-sm text-schemafy-text">
                    {project.name}
                  </TableCell>
                  <TableCell className="font-body-sm text-schemafy-dark-gray">
                    {project.updatedAt}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-4">
                      <span
                        className={
                          'px-15 py-1 rounded-2xl font-overline-sm bg-schemafy-secondary text-schemafy-text'
                        }
                      >
                        {project.myRole}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Menu
                      trigger={
                        <button className="relative p-2 hover:bg-schemafy-secondary rounded transition-colors">
                          <svg
                            width="4"
                            height="16"
                            viewBox="0 0 4 16"
                            fill="none"
                            className="text-schemafy-dark-gray"
                          >
                            <circle cx="2" cy="2" r="1.5" fill="currentColor" />
                            <circle cx="2" cy="8" r="1.5" fill="currentColor" />
                            <circle
                              cx="2"
                              cy="14"
                              r="1.5"
                              fill="currentColor"
                            />
                          </svg>
                        </button>
                      }
                    >
                      <MenuItem
                        onClick={() => {
                          handleDeleteProject(project.workspaceId, project.id);
                        }}
                        variant="destructive"
                      >
                        Delete
                      </MenuItem>
                    </Menu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        <div className="flex justify-center">
          <Pagination
            currentPage={currentPage}
            totalPages={projectStore.projects?.totalPages || 1}
            onPageChange={setCurrentPage}
          />
        </div>
      </div>

      <Dialog
        open={isWorkspaceDialogOpen}
        onOpenChange={setIsWorkspaceDialogOpen}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Workspace</DialogTitle>
          </DialogHeader>
          <div className="py-2 flex flex-col gap-8 justify-between items-center rounded-[10px]">
            <div className="w-full bg-schemafy-secondary rounded-xl">
              <input
                value={createWorkspaceName}
                onChange={(e) => setCreateWorkspaceName(e.target.value)}
                type="text"
                placeholder="Workspace name"
                className="w-full pl-3 pr-4 py-3 bg-schemafy-secondary rounded-xl font-body-xs text-schemafy-text placeholder-schemafy-dark-gray focus:outline-none focus:ring-2 focus:ring-schemafy-button-bg"
              />
            </div>
            <div className="w-full bg-schemafy-secondary rounded-xl">
              <input
                value={createWorkspaceDescription}
                onChange={(e) => setCreateWorkspaceDescription(e.target.value)}
                type="text"
                placeholder="Workspace description"
                className="w-full pl-3 pr-4 py-3 bg-schemafy-secondary rounded-xl font-body-xs text-schemafy-text placeholder-schemafy-dark-gray focus:outline-none focus:ring-2 focus:ring-schemafy-button-bg"
              />
            </div>
          </div>
          <div className="flex gap-2">
            <Button
              onClick={() =>
                handleCreateWorkSpace({
                  name: createWorkspaceName,
                  description: createWorkspaceDescription,
                })
              }
            >
              Save
            </Button>
            <Button
              variant="outline"
              onClick={() => setIsWorkspaceDialogOpen(false)}
            >
              Cancel
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={isProjectDialogOpen} onOpenChange={setIsProjectDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Project</DialogTitle>
          </DialogHeader>
          <div className="py-2 flex flex-col gap-8 justify-between items-center rounded-[10px]">
            <div className="w-full bg-schemafy-secondary rounded-xl">
              <input
                value={createProjectName}
                onChange={(e) => setCreateProjectName(e.target.value)}
                type="text"
                placeholder="Project name"
                className="w-full pl-3 pr-4 py-3 bg-schemafy-secondary rounded-xl font-body-xs text-schemafy-text placeholder-schemafy-dark-gray focus:outline-none focus:ring-2 focus:ring-schemafy-button-bg"
              />
            </div>
            <div className="w-full bg-schemafy-secondary rounded-xl">
              <input
                value={createProjectDescription}
                onChange={(e) => setCreateProjectDescription(e.target.value)}
                type="text"
                placeholder="Project description"
                className="w-full pl-3 pr-4 py-3 bg-schemafy-secondary rounded-xl font-body-xs text-schemafy-text placeholder-schemafy-dark-gray focus:outline-none focus:ring-2 focus:ring-schemafy-button-bg"
              />
            </div>
          </div>
          <div className="flex gap-2">
            <Button
              onClick={() =>
                handleCreateProject(projectStore.currentWorkspace?.id || '', {
                  name: createProjectName,
                  description: createProjectDescription,
                })
              }
            >
              Save
            </Button>
            <Button
              variant="outline"
              onClick={() => setIsWorkspaceDialogOpen(false)}
            >
              Cancel
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
});
