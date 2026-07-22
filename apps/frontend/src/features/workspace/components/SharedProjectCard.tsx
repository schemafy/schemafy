import { FolderKanban } from 'lucide-react';
import { useNavigate } from '@tanstack/react-router';
import { Button } from '@/components';
import type { ProjectSummaryResponse } from '@/features/project/api';
import { formatDate, toCapitalized } from '@/lib';
import { SharedProjectActions } from './SharedProjectActions';

interface SharedProjectCardProps {
  project: ProjectSummaryResponse;
  onLeaveClick: (project: ProjectSummaryResponse) => void;
}

export const SharedProjectCard = ({
  project,
  onLeaveClick,
}: SharedProjectCardProps) => {
  const navigate = useNavigate();

  return (
    <article className="schemafy-subtle-card flex flex-col gap-4 p-4">
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
        <SharedProjectActions
          project={project}
          onLeaveClick={onLeaveClick}
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
  );
};
