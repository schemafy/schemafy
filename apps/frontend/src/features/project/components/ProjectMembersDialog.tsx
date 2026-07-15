import { useState } from 'react';
import { Search } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  Pagination,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components';
import { useProjectMembers } from '@/features/project/hooks/useProjectMembers';
import { useProject } from '@/features/project/hooks/useProject';
import { availableRoles } from '@/features/project/utils/role';
import { formatDate, toCapitalized } from '@/lib';
import { authStore } from '@/store';
import { MemberRemoveButton } from './MemberRemoveButton';
import { ConfirmMemberRemoveDialog } from './ConfirmMemberRemoveDialog';
import type { ProjectMemberResponse } from '@/features/project/api';

interface ProjectMembersDialogProps {
  projectId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export const ProjectMembersDialog = ({
  projectId,
  open,
  onOpenChange,
}: ProjectMembersDialogProps) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [removeTarget, setRemoveTarget] =
    useState<ProjectMemberResponse | null>(null);

  const { project } = useProject(projectId);
  const currentUserRole = project?.currentUserRole ?? 'VIEWER';
  const roles = availableRoles(currentUserRole);
  const { user } = authStore;

  const {
    members: allMembers,
    membersData,
    removeMember,
    updateMemberRole,
  } = useProjectMembers(projectId, currentPage - 1);

  const members = allMembers.filter(
    (m) =>
      m.userName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.userEmail.toLowerCase().includes(searchQuery.toLowerCase()),
  );
  const totalPages = membersData?.totalPages ?? 1;

  const handleRoleChange = (userId: string, role: string) => {
    updateMemberRole(userId, { role });
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Project Members</DialogTitle>
          </DialogHeader>

          <div className="flex max-h-[min(80vh,600px)] flex-col gap-4 overflow-y-auto">
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

            <div className="schemafy-table-shell schemafy-scrollbar overflow-x-auto">
              <table className="w-full min-w-[500px]">
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
                    {currentUserRole === 'ADMIN' && (
                      <th className="w-10 px-5 py-3" />
                    )}
                  </tr>
                </thead>
                <tbody>
                  {members.length === 0 ? (
                    <tr>
                      <td
                        colSpan={currentUserRole === 'ADMIN' ? 5 : 4}
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
                            <span className="truncate font-body-sm text-schemafy-text">
                              {member.userName}
                            </span>
                          </div>
                        </td>
                        <td className="px-5 py-4 font-body-sm text-schemafy-dark-gray">
                          {member.userEmail}
                        </td>
                        <td className="px-5 py-4">
                          {currentUserRole === 'ADMIN' &&
                          user?.id !== member.userId ? (
                            <Select
                              value={member.role}
                              onValueChange={(role) =>
                                handleRoleChange(member.userId, role)
                              }
                            >
                              <SelectTrigger className="h-8 w-[6.25rem] px-3 py-0 font-body-xs">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                {roles.map((r) => (
                                  <SelectItem key={r} value={r}>
                                    {toCapitalized(r)}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          ) : (
                            <span className="schemafy-badge px-3 py-1 font-caption-md">
                              {toCapitalized(member.role)}
                            </span>
                          )}
                        </td>
                        <td className="text-nowrap px-5 py-4 font-body-sm text-schemafy-dark-gray">
                          {formatDate(new Date(member.joinedAt))}
                        </td>
                        {currentUserRole === 'ADMIN' &&
                          (user?.id !== member.userId ? (
                            <td className="px-5 py-4">
                              <MemberRemoveButton
                                member={member}
                                onRemoveClick={setRemoveTarget}
                              />
                            </td>
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
          </div>
        </DialogContent>
      </Dialog>

      <ConfirmMemberRemoveDialog
        open={!!removeTarget}
        onOpenChange={(open) => !open && setRemoveTarget(null)}
        member={removeTarget}
        onConfirm={() => {
          if (removeTarget) {
            removeMember(removeTarget.userId);
            setRemoveTarget(null);
          }
        }}
      />
    </>
  );
};
