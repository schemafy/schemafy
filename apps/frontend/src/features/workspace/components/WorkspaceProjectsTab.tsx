import { MoreHorizontal, Search } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, DropdownMenu, DropdownMenuContent, DropdownMenuTrigger, Pagination, } from '@/components';
import { useGetProjects, useLeaveProject } from "@/features/project/hooks/useProjects";
import { ProjectFormDialog } from "@/features/project/components/ProjectFormDialog";
import { ConfirmDialog } from './ConfirmDialog';
import type { ProjectSummaryResponse } from '@/features/project/api';

interface WorkspaceProjectsTabProps {
  workspaceId: string;
}

export const WorkspaceProjectsTab = ({workspaceId}: WorkspaceProjectsTabProps) => {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ProjectSummaryResponse | null>(null);
  const [leaveTarget, setLeaveTarget] = useState<ProjectSummaryResponse | null>(null);

  const {data: projects} = useGetProjects(workspaceId, currentPage - 1);

  const {mutate: leaveProject} = useLeaveProject();


  const filtered = projects?.content.filter((p) =>
    p.name.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  return (
    <div className="flex flex-col gap-4">
      <div className="relative">
        <Search
          size={16}
          className="absolute left-4 top-1/2 -translate-y-1/2 text-schemafy-dark-gray pointer-events-none"
        />
        <input
          type="text"
          placeholder="Search project"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full pl-10 pr-4 py-3 border border-schemafy-light-gray rounded-[12px] font-body-sm placeholder-schemafy-dark-gray bg-schemafy-bg focus:outline-none"
        />
      </div>

      <div className="w-full flex justify-end items-center">
        <Button size="sm" onClick={() => setIsCreateDialogOpen(true)}>Create</Button>
      </div>

      <div className="border border-schemafy-light-gray rounded-[12px] overflow-hidden">
        <table className="w-full">
          <thead>
          <tr className="border-b border-schemafy-light-gray">
            <th className="text-left px-6 py-4 font-overline-sm text-schemafy-text w-[40%]">
              Name
            </th>
            <th className="text-left px-6 py-4 font-overline-sm text-schemafy-text whitespace-nowrap">
              Last Modified
            </th>
            <th className="text-left px-6 py-4 font-overline-sm text-schemafy-text">
              Access
            </th>
            <th className="px-6 py-4 w-10"/>
          </tr>
          </thead>
          <tbody>
          {filtered && filtered.map((project) => (
            <tr
              key={project.id}
              className="border-b border-schemafy-light-gray last:border-b-0 hover:bg-schemafy-secondary transition-colors cursor-pointer"
              onClick={() => navigate(`/canvas/${project.id}`)}
            >
              <td className="px-6 py-4 font-body-sm text-schemafy-text">
                {project.name}
              </td>
              <td className="px-6 py-4 font-body-sm text-schemafy-dark-gray">
                {project.updatedAt}
              </td>
              <td className="px-6 py-4">
                  <span className="px-3 py-1 bg-schemafy-secondary text-schemafy-dark-gray font-body-sm rounded-full">
                    {project.myRole.toUpperCase()}
                  </span>
              </td>
              <ProjectOption
                project={project}
                onEditClick={setEditTarget}
                onLeaveClick={setLeaveTarget}
              />
            </tr>
          ))}
          <tr>
            <td colSpan={5} className="py-2">
              <div className="flex justify-center">
                <Pagination
                  currentPage={currentPage}
                  totalPages={projects?.totalPages ?? 1}
                  onPageChange={setCurrentPage}
                />
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <ProjectFormDialog
        open={isCreateDialogOpen}
        onOpenChange={setIsCreateDialogOpen}
        mode='create'
        workspaceId={workspaceId}
      />

      <ProjectFormDialog
        open={!!editTarget}
        onOpenChange={(open) => !open && setEditTarget(null)}
        mode='edit'
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
        onConfirm={() => leaveTarget && leaveProject(leaveTarget.id)}
      />
    </div>
  );
};

const ProjectOption = ({
                         project,
                         onEditClick,
                         onLeaveClick,
                       }: {
  project: ProjectSummaryResponse;
  onEditClick: (project: ProjectSummaryResponse) => void;
  onLeaveClick: (project: ProjectSummaryResponse) => void;
}) => {
  return <td className="px-6 py-4" onClick={(e) => e.stopPropagation()}>
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button className="text-schemafy-dark-gray hover:text-schemafy-text transition-colors">
          <MoreHorizontal size={16}/>
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        sideOffset={4}
        align="end"
        className="!p-1.5 !min-w-0 flex flex-col gap-0.5"
      >
        {project.myRole === 'ADMIN' && <Button
          variant="none"
          size="none"
          className="font-caption-md px-2 py-1 whitespace-nowrap"
          onClick={() => onEditClick(project)}
        >
          Edit
        </Button>}
        <Button
          variant="none"
          size="none"
          className="text-schemafy-destructive font-caption-md px-2 py-1 whitespace-nowrap"
          onClick={() => onLeaveClick(project)}
        >
          Leave
        </Button>
      </DropdownMenuContent>
    </DropdownMenu>
  </td>;
}