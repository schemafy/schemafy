import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  Menu,
  MenuItem,
} from '@/components';
import type { ProjectSummary } from '@/lib/api';

interface ProjectTableProps {
  projects: ProjectSummary[];
  onDelete: (workspaceId: string, projectId: string) => void;
}

export const ProjectTable = ({ projects, onDelete }: ProjectTableProps) => {
  return (
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
          {projects.map((project) => (
            <TableRow key={project.id}>
              <TableCell className="font-body-sm text-schemafy-text">
                {project.name}
              </TableCell>
              <TableCell className="font-body-sm text-schemafy-dark-gray">
                {project.updatedAt}
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-4">
                  <span className="px-15 py-1 rounded-2xl font-overline-sm bg-schemafy-secondary text-schemafy-text">
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
                        <circle cx="2" cy="14" r="1.5" fill="currentColor" />
                      </svg>
                    </button>
                  }
                >
                  <MenuItem
                    onClick={() => onDelete(project.workspaceId, project.id)}
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
  );
};
