import { useNavigate } from '@tanstack/react-router';
import type { ProjectSummaryResponse } from '@/features/project/api';
import { formatDate, toCapitalized } from '@/lib';
import { SharedProjectActions } from './SharedProjectActions';

interface SharedProjectRowProps {
  project: ProjectSummaryResponse;
  onLeaveClick: (project: ProjectSummaryResponse) => void;
}

export const SharedProjectRow = ({
  project,
  onLeaveClick,
}: SharedProjectRowProps) => {
  const navigate = useNavigate();

  return (
    <tr
      className='cursor-pointer border-b border-schemafy-glass-border transition-colors last:border-b-0 hover:bg-schemafy-secondary/40'
      onClick={() =>
        void navigate({
          to: '/project/$projectId',
          params: { projectId: project.id },
        })
      }
    >
      <td className='px-5 py-4'>
        <div className='flex min-w-0 items-center gap-3'>
          <div className='min-w-0'>
            <p className='truncate font-heading-xs text-schemafy-text'>
              {project.name}
            </p>
            {project.description && (
              <p className='truncate font-caption-md text-schemafy-dark-gray'>
                {project.description}
              </p>
            )}
          </div>
        </div>
      </td>
      <td className='px-5 py-4 font-body-sm text-schemafy-dark-gray'>
        {formatDate(new Date(project.updatedAt))}
      </td>
      <td className='px-5 py-4'>
        <span className='schemafy-badge px-3 py-1 font-body-sm'>
          {toCapitalized(project.myRole)}
        </span>
      </td>
      <td className='px-5 py-4' onClick={(e) => e.stopPropagation()}>
        <SharedProjectActions project={project} onLeaveClick={onLeaveClick} />
      </td>
    </tr>
  );
};
