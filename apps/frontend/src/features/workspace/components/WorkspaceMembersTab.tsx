import { useState } from 'react';
import { MoreHorizontal, Search, UsersRound } from 'lucide-react';
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
  LoadingState,
  Pagination,
} from '@/components';
import { ChangeRoleDialog } from './ChangeRoleDialog';
import { ConfirmDialog } from './ConfirmDialog';
import { useWorkspaceMembers } from '../hooks/useWorkspaceMembers';
import { formatDate, toCapitalized } from '@/lib';
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

  const {
    members: workspaceMembers,
    membersData,
    isLoadingMembers,
    removeMember,
    updateMemberRole,
    isUpdatingMemberRole,
  } = useWorkspaceMembers(workspaceId, currentPage - 1);

  const { user } = authStore;

  const members = workspaceMembers.filter(
    (m) =>
      m.userName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.userEmail.toLowerCase().includes(searchQuery.toLowerCase()),
  );
  const totalPages = membersData?.totalPages ?? 1;

  const roles = availableRoles(currentUserRole);

  const handleOpenRoleChange = (userId: string, currentRole: string) => {
    setRoleChangeTarget({ userId, currentRole });
    setSelectedRole(currentRole);
  };

  const handleRoleChange = () => {
    if (!roleChangeTarget || !selectedRole) return;

    const successAction = {
      onSuccess: () => {
        setRoleChangeTarget(null);
      },
    };

    updateMemberRole(
      roleChangeTarget.userId,
      {
        role: selectedRole,
      },
      successAction,
    );
  };

  return (
    <div className="flex flex-col gap-4">
      <div>
        <div className="relative">
          <Search
            size={16}
            className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-schemafy-dark-gray"
          />
          <input
            type="text"
            placeholder="Search member"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="schemafy-input h-11 w-full pl-10 pr-4 font-body-sm"
          />
        </div>
      </div>

      <div className="grid gap-3 md:hidden">
        {isLoadingMembers ? (
          <LoadingState className="min-h-[200px]" label="Loading members..." />
        ) : (
          members.map((member) => (
            <article
              key={member.userId}
              className="schemafy-subtle-card flex flex-col gap-4 p-4"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex min-w-0 items-start gap-3">
                  <MemberAvatar name={member.userName} />
                  <div className="min-w-0">
                    <h3 className="truncate font-heading-sm text-schemafy-text">
                      {member.userName}
                    </h3>
                    <p className="mt-1 truncate font-caption-md text-schemafy-dark-gray">
                      {member.userEmail}
                    </p>
                  </div>
                </div>
                {currentUserRole === 'ADMIN' && user?.id !== member.userId && (
                  <MemberActions
                    handleOpenRoleChange={handleOpenRoleChange}
                    onRemoveClick={setRemoveTarget}
                    member={member}
                  />
                )}
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="schemafy-badge px-3 py-1 font-caption-md">
                  {toCapitalized(member.role)}
                </span>
                <span className="font-caption-md text-schemafy-dark-gray">
                  Joined {formatDate(new Date(member.joinedAt))}
                </span>
              </div>
            </article>
          ))
        )}
      </div>

      <div className="schemafy-table-shell schemafy-scrollbar hidden overflow-x-auto md:block">
        <table className="w-full min-w-[720px]">
          <thead>
            <tr className="border-b border-schemafy-glass-border bg-schemafy-secondary/50">
              <th className="w-[35%] px-5 py-3 text-left font-overline-sm text-schemafy-text">
                Name
              </th>
              <th className="px-5 py-3 text-left font-overline-sm text-schemafy-text">
                Email
              </th>
              <th className="px-5 py-3 text-left font-overline-sm text-schemafy-text">
                Role
              </th>
              <th className="px-5 py-3 text-left font-overline-sm text-schemafy-text">
                Joined
              </th>
              {currentUserRole === 'ADMIN' && <th className="w-10 px-5 py-3" />}
            </tr>
          </thead>
          <tbody>
            {isLoadingMembers ? (
              <tr>
                <td colSpan={5} className="px-5 py-8">
                  <LoadingState
                    className="min-h-[120px]"
                    label="Loading members..."
                  />
                </td>
              </tr>
            ) : members.length === 0 ? (
              <tr>
                <td
                  colSpan={5}
                  className="px-5 py-10 text-center font-body-sm text-schemafy-dark-gray"
                >
                  No members match your search.
                </td>
              </tr>
            ) : (
              members.map((member) => (
                <tr
                  key={member.userId}
                  className="border-b border-schemafy-glass-border transition-colors last:border-b-0 hover:bg-schemafy-secondary/40"
                >
                  <td className="px-5 py-4">
                    <div className="flex min-w-0 items-center gap-3">
                      <MemberAvatar name={member.userName} />
                      <span className="truncate font-body-sm text-schemafy-text">
                        {member.userName}
                      </span>
                    </div>
                  </td>
                  <td className="px-5 py-4 font-body-sm text-schemafy-dark-gray">
                    {member.userEmail}
                  </td>
                  <td className="px-5 py-4">
                    <span className="schemafy-badge px-3 py-1 font-caption-md">
                      {toCapitalized(member.role)}
                    </span>
                  </td>
                  <td className="text-nowrap px-5 py-4 font-body-sm text-schemafy-dark-gray">
                    {formatDate(new Date(member.joinedAt))}
                  </td>
                  {currentUserRole === 'ADMIN' &&
                    (user?.id !== member.userId ? (
                      <MemberOptions
                        handleOpenRoleChange={handleOpenRoleChange}
                        onRemoveClick={setRemoveTarget}
                        member={member}
                      />
                    ) : (
                      <td className="px-5 py-4" />
                    ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="flex justify-center">
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </div>

      <ChangeRoleDialog
        open={!!roleChangeTarget}
        onOpenChange={(open) => !open && setRoleChangeTarget(null)}
        selectedRole={selectedRole}
        onSelectedRoleChange={setSelectedRole}
        availableRoles={roles}
        onSave={handleRoleChange}
        isPending={isUpdatingMemberRole}
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

const MemberAvatar = ({ name }: { name: string }) => {
  const initials = name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-schemafy-glass-border bg-schemafy-secondary font-caption-md text-schemafy-text">
      {initials || <UsersRound className="h-4 w-4 text-schemafy-soft-blue" />}
    </span>
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
    <td className="px-5 py-4">
      <MemberActions
        handleOpenRoleChange={handleOpenRoleChange}
        onRemoveClick={onRemoveClick}
        member={member}
      />
    </td>
  );
};

const MemberActions = ({
  handleOpenRoleChange,
  onRemoveClick,
  member,
}: {
  handleOpenRoleChange: (userId: string, currentRole: string) => void;
  onRemoveClick: (member: WorkspaceMemberResponse) => void;
  member: WorkspaceMemberResponse;
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
        <Button
          variant="none"
          size="none"
          className="schemafy-menu-button whitespace-nowrap px-3 py-2 text-left font-caption-md"
          onClick={() => {
            handleOpenRoleChange(member.userId, member.role);
          }}
        >
          Change Role
        </Button>
        <Button
          variant="none"
          size="none"
          className="schemafy-menu-button whitespace-nowrap px-3 py-2 text-left font-caption-md text-schemafy-destructive"
          onClick={() => onRemoveClick(member)}
        >
          Delete
        </Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
