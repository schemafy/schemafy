import { FolderKanban, MoreHorizontal, Plus, Search } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
  LoadingState,
  Pagination,
} from '@/components';
import { useProjects } from '@/features/project/hooks/useProjects';
import { ProjectFormDialog } from '@/features/project/components/ProjectFormDialog';
import { ConfirmDialog } from './ConfirmDialog';
import { formatDate, toCapitalized } from '@/lib';
import type { ProjectSummaryResponse } from '@/features/project/api';

interface WorkspaceProjectsTabProps {
  workspaceId: string;
  currentUserRole: string;
}

export const WorkspaceProjectsTab = ({
  workspaceId,
  currentUserRole,
}: WorkspaceProjectsTabProps) => {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ProjectSummaryResponse | null>(
    null,
  );
  const [leaveTarget, setLeaveTarget] = useState<ProjectSummaryResponse | null>(
    null,
  );

  const {
    projects,
    projectsData,
    isLoadingProjects,
    leaveProject,
  } = useProjects(
    workspaceId,
    currentPage - 1,
  );

  const filtered = projects.filter((p) =>
    p.name.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  return (
        <div className="flex flex-col gap-4">
          {isLoadingProjects ? (
            <LoadingState className="min-h-[200px]" label="Loading projects..." />
          ) : (
            <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative min-w-0 flex-1">
          <Search
            size={16}
            className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-schemafy-dark-gray"
          />
          <input
            type="text"
            placeholder="Search project"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="schemafy-input h-11 w-full pl-10 pr-4 font-body-sm"
          />
        </div>
        {currentUserRole === 'ADMIN' && (
          <Button
            className="h-11 w-full px-4 sm:w-auto"
            onClick={() => setIsCreateDialogOpen(true)}
          >
            <Plus className="h-4 w-4" />
            Create Project
          </Button>
        )}
      </div>

      <div className="grid gap-3 md:hidden">
        {filtered.length === 0 ? (
          <EmptyProjectsState />
        ) : (
          filtered.map((project) => (
            <article
              key={project.id}
              className="schemafy-subtle-card flex flex-col gap-4 p-4"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex min-w-0 items-start gap-3">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-schemafy-glass-border bg-schemafy-secondary text-schemafy-soft-blue">
                    <FolderKanban className="h-4 w-4" />
                  </span>
                  <div className="min-w-0">
                    <h3 className="truncate font-heading-sm text-schemafy-text">
                      {project.name}
                    </h3>
                    {project.description && (
                      <p className="mt-1 line-clamp-2 font-body-sm text-schemafy-dark-gray">
                        {project.description}
                      </p>
                    )}
                    <p className="mt-1 font-caption-md text-schemafy-dark-gray">
                      Updated {formatDate(new Date(project.updatedAt))}
                    </p>
                  </div>
                </div>
                <ProjectActions
                  project={project}
                  onEditClick={setEditTarget}
                  onLeaveClick={setLeaveTarget}
                />
              </div>
              <div className="flex items-center justify-between gap-3">
                <span className="schemafy-badge px-3 py-1 font-caption-md">
                  {toCapitalized(project.myRole)}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  className="h-9 px-4"
                  onClick={() =>
                    void navigate({
                      to: '/project/$projectId',
                      params: { projectId: project.id },
                    })
                  }
                >
                  Open
                </Button>
              </div>
            </article>
          ))
        )}
      </div>

      <div className="schemafy-table-shell schemafy-scrollbar hidden overflow-x-auto md:block">
        <table className="w-full min-w-[600px]">
          <thead>
            <tr className="border-b border-schemafy-glass-border bg-schemafy-secondary/50">
              <th className="w-[46%] px-5 py-3 text-left font-overline-sm text-schemafy-text">
                Project
              </th>
              <th className="whitespace-nowrap px-5 py-3 text-left font-overline-sm text-schemafy-text">
                Last Modified
              </th>
              <th className="px-5 py-3 text-left font-overline-sm text-schemafy-text">
                Access
              </th>
              <th className="w-10 px-5 py-3" />
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td
                  colSpan={4}
                  className="px-5 py-10 text-center font-body-sm text-schemafy-dark-gray"
                >
                  No projects match your search.
                </td>
              </tr>
            ) : (
              filtered.map((project) => (
                <tr
                  key={project.id}
                  className="cursor-pointer border-b border-schemafy-glass-border transition-colors last:border-b-0 hover:bg-schemafy-secondary/40"
                  onClick={() =>
                    void navigate({
                      to: '/project/$projectId',
                      params: { projectId: project.id },
                    })
                  }
                >
                  <td className="px-5 py-4">
                    <div className="flex min-w-0 items-center gap-3">
                      <div className="min-w-0">
                        <p className="truncate font-heading-xs text-schemafy-text">
                          {project.name}
                        </p>
                        {project.description && (
                          <p className="truncate font-caption-md text-schemafy-dark-gray">
                            {project.description}
                          </p>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-5 py-4 font-body-sm text-schemafy-dark-gray">
                    {formatDate(new Date(project.updatedAt))}
                  </td>
                  <td className="px-5 py-4">
                    <span className="schemafy-badge px-3 py-1 font-body-sm">
                      {toCapitalized(project.myRole)}
                    </span>
                  </td>
                  <ProjectOption
                    project={project}
                    onEditClick={setEditTarget}
                    onLeaveClick={setLeaveTarget}
                  />
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="flex justify-center">
        <Pagination
          currentPage={currentPage}
          totalPages={projectsData?.totalPages ?? 1}
          onPageChange={setCurrentPage}
        />
      </div>

      <ProjectFormDialog
        open={isCreateDialogOpen}
        onOpenChange={setIsCreateDialogOpen}
        mode="create"
        workspaceId={workspaceId}
      />

      <ProjectFormDialog
        open={!!editTarget}
        onOpenChange={(open) => !open && setEditTarget(null)}
        mode="edit"
        workspaceId={workspaceId}
        projectId={editTarget?.id}
        initialName={editTarget?.name}
        initialDescription={editTarget?.description}
      />

      <ConfirmDialog
        open={!!leaveTarget}
        onOpenChange={(open) => !open && setLeaveTarget(null)}
        title="Leave Project"
        description={`Would you like to leave ${leaveTarget?.name}?`}
        confirmLabel="Leave"
        onConfirm={async () => {
          if (!leaveTarget) return;
          const targetId = leaveTarget.id;
          setLeaveTarget(null);
          const result = await leaveProject(targetId);
          const totalPages = result.data?.totalPages ?? 0;
          if (currentPage > totalPages && totalPages > 0) {
            setCurrentPage(totalPages);
          }
        }}
      />
    </div>
      )}
    </div>
  );
};

const EmptyProjectsState = () => (
  <div className="schemafy-subtle-card px-4 py-8 text-center font-body-sm text-schemafy-dark-gray">
    No projects match your search.
  </div>
);

const ProjectOption = ({
  project,
  onEditClick,
  onLeaveClick,
}: {
  project: ProjectSummaryResponse;
  onEditClick: (project: ProjectSummaryResponse) => void;
  onLeaveClick: (project: ProjectSummaryResponse) => void;
}) => {
  return (
    <td className="px-5 py-4" onClick={(e) => e.stopPropagation()}>
      <ProjectActions
        project={project}
        onEditClick={onEditClick}
        onLeaveClick={onLeaveClick}
      />
    </td>
  );
};

const ProjectActions = ({
  project,
  onEditClick,
  onLeaveClick,
}: {
  project: ProjectSummaryResponse;
  onEditClick: (project: ProjectSummaryResponse) => void;
  onLeaveClick: (project: ProjectSummaryResponse) => void;
}) => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="schemafy-icon-button schemafy-focus-ring flex h-8 w-8 items-center justify-center"
        >
          <MoreHorizontal size={16} />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        sideOffset={4}
        align="end"
        className="!min-w-0 flex flex-col gap-0.5 !p-1.5"
      >
        {project.myRole === 'ADMIN' && (
          <Button
            variant="none"
            size="none"
            className="schemafy-menu-button whitespace-nowrap px-3 py-2 font-caption-md"
            onClick={() => onEditClick(project)}
          >
            Edit
          </Button>
        )}
        <Button
          variant="none"
          size="none"
          className="schemafy-menu-button whitespace-nowrap px-3 py-2 font-caption-md text-schemafy-destructive"
          onClick={() => onLeaveClick(project)}
        >
          Leave
        </Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
