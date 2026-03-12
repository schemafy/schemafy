import { MoreHorizontal, Search } from 'lucide-react';
import { useState } from 'react';
import { Button, DropdownMenu, DropdownMenuContent, DropdownMenuTrigger, Pagination, } from '@/components';

type Project = {
  id: string;
  name: string;
  lastModified: string;
  access: 'Private' | 'Shared';
  memberCount: number;
};

const MOCK_PROJECTS: Project[] = [
  {
    id: '1',
    name: 'Project Alpha',
    lastModified: '2024-01-15',
    access: 'Private',
    memberCount: 2,
  },
  {
    id: '2',
    name: 'Project Beta',
    lastModified: '2024-02-20',
    access: 'Shared',
    memberCount: 3,
  },
  {
    id: '3',
    name: 'Project Gamma',
    lastModified: '2024-03-10',
    access: 'Private',
    memberCount: 1,
  },
];

const TOTAL_PAGES = 100;

export const WorkspaceProjectsTab = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const filtered = MOCK_PROJECTS.filter((p) =>
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
            <th className="text-left px-6 py-4 font-overline-sm text-schemafy-text">
              Members
            </th>
            <th className="px-6 py-4 w-10"/>
          </tr>
          </thead>
          <tbody>
          {filtered.map((project) => (
            <tr
              key={project.id}
              className="border-b border-schemafy-light-gray last:border-b-0 hover:bg-schemafy-secondary transition-colors"
            >
              <td className="px-6 py-4 font-body-sm text-schemafy-text">
                {project.name}
              </td>
              <td className="px-6 py-4 font-body-sm text-schemafy-dark-gray">
                {project.lastModified}
              </td>
              <td className="px-6 py-4">
                  <span className="px-3 py-1 bg-schemafy-secondary text-schemafy-dark-gray font-body-sm rounded-full">
                    {project.access}
                  </span>
              </td>
              <td className="px-6 py-4">
                <div
                  className="w-6 h-6 rounded-full bg-schemafy-secondary flex items-center justify-center font-overline-xs text-schemafy-dark-gray">
                  {project.memberCount}
                </div>
              </td>
              <td className="px-6 py-4">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <button className="text-schemafy-dark-gray hover:text-schemafy-text transition-colors">
                      <MoreHorizontal size={16}/>
                    </button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent
                    sideOffset={4}
                    align="end"
                    className="!p-1.5 !min-w-0"
                  >
                    <Button
                      variant="none"
                      size="none"
                      className="text-schemafy-destructive font-caption-md px-2 py-1 whitespace-nowrap"
                    >
                      Delete
                    </Button>
                  </DropdownMenuContent>
                </DropdownMenu>
              </td>
            </tr>
          ))}
          <tr>
            <td colSpan={5} className="py-2">
              <div className="flex justify-center">
                <Pagination
                  currentPage={currentPage}
                  totalPages={TOTAL_PAGES}
                  onPageChange={setCurrentPage}
                />
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
};
