import { Button, DropdownMenu, DropdownMenuContent, DropdownMenuTrigger, Pagination } from '@/components';
import { useState } from "react";
import { MoreHorizontal, Search } from "lucide-react";

type Member = {
  id: string;
  name: string;
  email: string;
  role: string;
  joinedAt: string;
};

const MOCK_MEMBERS: Member[] = [
  {
    id: '1',
    name: 'Alice Kim',
    email: 'alice@example.com',
    role: 'admin',
    joinedAt: '2024-01-01',
  },
  {
    id: '2',
    name: 'Bob Lee',
    email: 'bob@example.com',
    role: 'member',
    joinedAt: '2024-02-10',
  },
  {
    id: '3',
    name: 'Carol Park',
    email: 'carol@example.com',
    role: 'member',
    joinedAt: '2024-03-05',
  },
  {
    id: '4',
    name: 'David Choi',
    email: 'david@example.com',
    role: 'editor',
    joinedAt: '2024-03-20',
  },
];

const TOTAL_PAGES = 3;

export const WorkspaceMembersTab = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  return (
    <div className="flex flex-col gap-4">
      <div className="relative">
        <Search
          size={16}
          className="absolute left-4 top-1/2 -translate-y-1/2 text-schemafy-dark-gray pointer-events-none"
        />
        <input
          type="text"
          placeholder="Search member"
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
            <th className="text-left px-6 py-4 font-overline-sm text-schemafy-text">
              Email
            </th>
            <th className="text-left px-6 py-4 font-overline-sm text-schemafy-text">
              Role
            </th>
            <th className="text-left px-6 py-4 font-overline-sm text-schemafy-text">
              Joined
            </th>
          </tr>
          </thead>
          <tbody>
          {MOCK_MEMBERS.map((member) => (
            <tr
              key={member.id}
              className="border-b border-schemafy-light-gray last:border-b-0 hover:bg-schemafy-secondary transition-colors"
            >
              <td className="px-6 py-4 font-body-sm text-schemafy-text">
                {member.name}
              </td>
              <td className="px-6 py-4 font-body-sm text-schemafy-dark-gray">
                {member.email}
              </td>
              <td className="px-6 py-4">
                <span className="px-3 py-1 bg-schemafy-secondary text-schemafy-dark-gray font-caption-md rounded-full">
                  {member.role}
                </span>
              </td>
              <td className="px-6 py-4 font-body-sm text-schemafy-dark-gray text-nowrap">
                {member.joinedAt}
              </td>
              <td className="px-6 py-4">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <button
                      className="text-schemafy-dark-gray hover:text-schemafy-text transition-colors"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <MoreHorizontal size={16}/>
                    </button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent sideOffset={4} align="end" className="!p-1.5 !min-w-0">
                    <Button
                      variant="none"
                      size="none"
                      className="text-schemafy-destructive font-caption-md px-2 py-1 whitespace-nowrap"
                      onClick={(e) => e.stopPropagation()}
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