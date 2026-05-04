import { useState } from 'react';
import { MoreHorizontal, Search } from 'lucide-react';
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
  Pagination,
} from '@/components';
import { ChangeRoleDialog } from './ChangeRoleDialog';
import { ConfirmDialog } from './ConfirmDialog';
import {
  useGetMembers,
  useRemoveMember,
  useUpdateMemberRole,
} from '../hooks/useWorkspaces';
import { formatDateWithTime } from '@/lib';
import { availableRoles } from '@/features/workspace/utils/role';
import type { WorkspaceMemberResponse } from '@/features/workspace/api';
import { authStore } from '@/store';

interface WorkspaceMembersTabProps {
  workspaceId: string;
  currentUserRole: string;
}

export const WorkspaceMembersTab = ({
  workspaceId,
  currentUserRole,
}: WorkspaceMembersTabProps) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [roleChangeTarget, setRoleChangeTarget] = useState<{
    userId: string;
    currentRole: string;
  } | null>(null);
  const [selectedRole, setSelectedRole] = useState('');
  const [removeTarget, setRemoveTarget] =
    useState<WorkspaceMemberResponse | null>(null);

  const { data, isLoading } = useGetMembers(workspaceId, currentPage - 1);
  const { mutate: removeMember } = useRemoveMember(workspaceId);
  const { mutate: updateMemberRole, isPending: isUpdatingRole } =
    useUpdateMemberRole(workspaceId);

  const { user } = authStore;

  const members = (data?.content ?? []).filter(
    (m) =>
      m.userName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.userEmail.toLowerCase().includes(searchQuery.toLowerCase()),
  );
  const totalPages = data?.totalPages ?? 1;

  const roles = availableRoles(currentUserRole);

  const handleOpenRoleChange = (userId: string, currentRole: string) => {
    setRoleChangeTarget({ userId, currentRole });
    setSelectedRole(currentRole);
  };

  const handleRoleChange = () => {
    if (!roleChangeTarget || !selectedRole) return;

    const variableData = {
      userId: roleChangeTarget.userId,
      data: {
        role: selectedRole,
      },
    };

    const successAction = {
      onSuccess: () => {
        setRoleChangeTarget(null);
      },
    };

    updateMemberRole(variableData, successAction);
  };

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
              <th className="text-left px-6 py-4 font-overline-sm text-schemafy-text w-[35%]">
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
              {currentUserRole === 'ADMIN' && <th className="px-6 py-4 w-10" />}
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td
                  colSpan={5}
                  className="px-6 py-8 text-center font-body-sm text-schemafy-dark-gray"
                >
                  Loading...
                </td>
              </tr>
            ) : (
              members.map((member) => (
                <tr
                  key={member.userId}
                  className="border-b border-schemafy-light-gray last:border-b-0 hover:bg-schemafy-secondary transition-colors"
                >
                  <td className="px-6 py-4 font-body-sm text-schemafy-text">
                    {member.userName}
                  </td>
                  <td className="px-6 py-4 font-body-sm text-schemafy-dark-gray">
                    {member.userEmail}
                  </td>
                  <td className="px-6 py-4">
                    <span className="px-3 py-1 bg-schemafy-secondary text-schemafy-dark-gray font-caption-md rounded-full">
                      {member.role}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-body-sm text-schemafy-dark-gray text-nowrap">
                    {formatDateWithTime(new Date(member.joinedAt))}
                  </td>
                  {currentUserRole === 'ADMIN' &&
                    user?.id !== member.userId && (
                      <MemberOptions
                        handleOpenRoleChange={handleOpenRoleChange}
                        onRemoveClick={setRemoveTarget}
                        member={member}
                      />
                    )}
                </tr>
              ))
            )}
            <tr>
              <td colSpan={5} className="py-2">
                <div className="flex justify-center">
                  <Pagination
                    currentPage={currentPage}
                    totalPages={totalPages}
                    onPageChange={setCurrentPage}
                  />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <ChangeRoleDialog
        open={!!roleChangeTarget}
        onOpenChange={(open) => !open && setRoleChangeTarget(null)}
        selectedRole={selectedRole}
        onSelectedRoleChange={setSelectedRole}
        availableRoles={roles}
        onSave={handleRoleChange}
        isPending={isUpdatingRole}
      />

      <ConfirmDialog
        open={!!removeTarget}
        onOpenChange={(open) => !open && setRemoveTarget(null)}
        title="Remove Member"
        description={`Would you like to remove ${removeTarget?.userName}(${removeTarget?.userEmail}) from the workspace?`}
        confirmLabel="Remove"
        onConfirm={() => removeTarget && removeMember(removeTarget.userId)}
      />
    </div>
  );
};

const MemberOptions = ({
  handleOpenRoleChange,
  onRemoveClick,
  member,
}: {
  handleOpenRoleChange: (userId: string, currentRole: string) => void;
  onRemoveClick: (member: WorkspaceMemberResponse) => void;
  member: WorkspaceMemberResponse;
}) => {
  return (
    <td className="px-6 py-4">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="text-schemafy-dark-gray hover:text-schemafy-text transition-colors">
            <MoreHorizontal size={16} />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent
          sideOffset={4}
          align="end"
          className="!p-1.5 !min-w-0 flex flex-col gap-0.5"
        >
          <Button
            variant="none"
            size="none"
            className="font-caption-md px-2 py-1 whitespace-nowrap text-left"
            onClick={() => {
              handleOpenRoleChange(member.userId, member.role);
            }}
          >
            Change Role
          </Button>
          <Button
            variant="none"
            size="none"
            className="text-schemafy-destructive font-caption-md px-2 py-1 whitespace-nowrap text-left"
            onClick={() => onRemoveClick(member)}
          >
            Delete
          </Button>
        </DropdownMenuContent>
      </DropdownMenu>
    </td>
  );
};
