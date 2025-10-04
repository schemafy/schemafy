import { useState } from 'react';
import {
  Dropdown,
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
} from '@/components';

interface Project {
  id: string;
  name: string;
  lastModified: string;
  access: 'Private' | 'Shared';
  members: number;
}

const mockProjects: Project[] = [
  {
    id: '1',
    name: 'Project Alpha',
    lastModified: '2024-01-15',
    access: 'Private',
    members: 2,
  },
  {
    id: '2',
    name: 'Project Beta',
    lastModified: '2024-02-20',
    access: 'Shared',
    members: 1,
  },
  {
    id: '3',
    name: 'Project Gamma',
    lastModified: '2024-03-10',
    access: 'Private',
    members: 1,
  },
];

export const ProjectsPage = () => {
  const [selectedWorkspace, setSelectedWorkspace] =
    useState("Workspace's name");
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [projects] = useState<Project[]>(mockProjects);

  const workspaceOptions = ["Workspace's name", 'Workspace 2', 'Workspace 3'];

  const filteredProjects = projects.filter((project) =>
    project.name.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  return (
    <div className="min-h-screen bg-schemafy-bg">
      <div className="flex flex-col">
        <div className="my-5">
          <Dropdown
            value={selectedWorkspace}
            options={workspaceOptions}
            onChange={setSelectedWorkspace}
          />
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
          <Button>New Project</Button>
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
              {filteredProjects.map((project) => (
                <TableRow key={project.id}>
                  <TableCell className="font-body-sm text-schemafy-text">
                    {project.name}
                  </TableCell>
                  <TableCell className="font-body-sm text-schemafy-dark-gray">
                    {project.lastModified}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-4">
                      <span
                        className={
                          'px-15 py-1 rounded-2xl font-overline-sm bg-schemafy-secondary text-schemafy-text'
                        }
                      >
                        {project.access}
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
                      <MenuItem onClick={() => {}} variant="destructive">
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
            totalPages={3}
            onPageChange={setCurrentPage}
          />
        </div>
      </div>
    </div>
  );
};
