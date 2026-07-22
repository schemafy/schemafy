import { Search } from 'lucide-react';
import { useState } from 'react';
import {
  LoadingState,
  Pagination,
} from '@/components';
import type { ProjectSummaryResponse } from '@/features/project/api';
import { useMySharedProjects } from '@/features/project/hooks/useMySharedProjects';
import { ConfirmDialog } from './ConfirmDialog';
import { SharedProjectCard } from './SharedProjectCard';
import { SharedProjectRow } from './SharedProjectRow';

export const SharedProjectsTab = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [leaveTarget, setLeaveTarget] = useState<ProjectSummaryResponse | null>(
    null,
  );

  const { projects, projectsData, isLoadingProjects, leaveProject } =
    useMySharedProjects(currentPage - 1);

  const filtered = projects.filter((project) =>
    project.name.toLowerCase().includes(searchQuery.toLowerCase()),
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
          </div>
          <div className="grid gap-3 md:hidden">
            {filtered.length === 0 ? (
              <div className="schemafy-subtle-card px-4 py-8 text-center font-body-sm text-schemafy-dark-gray">
                No projects match your search.
              </div>
            ) : (
              filtered.map((project) => (
                <SharedProjectCard
                  key={project.id}
                  project={project}
                  onLeaveClick={setLeaveTarget}
                />
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
                    <SharedProjectRow
                      key={project.id}
                      project={project}
                      onLeaveClick={setLeaveTarget}
                    />
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


