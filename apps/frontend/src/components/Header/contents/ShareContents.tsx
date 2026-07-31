import { useState } from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';
import { Avatar } from '../../Avatar';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../Select';
import { useProject } from '@/features/project/hooks/useProject';
import { useProjectInvitations } from '@/features/project/hooks/useProjectInvitations';
import { useProjectMembers } from '@/features/project/hooks/useProjectMembers';
import { ProjectMembersDialog } from '@/features/project/components/ProjectMembersDialog';
import { MemberRemoveButton } from '@/features/project/components/MemberRemoveButton';
import { ConfirmMemberRemoveDialog } from '@/features/project/components/ConfirmMemberRemoveDialog';
import { availableRoles } from '@/features/project/utils/role';
import { authStore } from '@/store';
import { cn, toCapitalized } from '@/lib';
import { Share2, Users } from 'lucide-react';
import type { ProjectMemberResponse } from '@/features/project/api';

const MEMBER_PREVIEW_SIZE = 5;

const RoleText = ({ role }: { role: string }) => {
  return (
    <span className="font-body-xs text-schemafy-dark-gray">
      {toCapitalized(role)}
    </span>
  );
};

const RoleSelect = ({
  value,
  onValueChange,
  userRole = 'ADMIN',
  className,
}: {
  value: string;
  onValueChange: (value: string) => void;
  userRole?: string;
  className?: string;
}) => {
  const roles = availableRoles(userRole);

  return (
    <Select value={value} onValueChange={onValueChange}>
      <SelectTrigger
        className={cn('h-9 w-[6.25rem] px-3 py-0 font-body-xs', className)}
      >
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          {roles.map((role) => (
            <SelectItem key={role} value={role}>
              {toCapitalized(role)}
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
};

export const ShareContents = ({ projectId }: { projectId: string }) => {
  const [email, setEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('VIEWER');
  const [isMembersDialogOpen, setIsMembersDialogOpen] = useState(false);
  const [removeTarget, setRemoveTarget] =
    useState<ProjectMemberResponse | null>(null);

  const { project } = useProject(projectId);
  const { members, membersData, updateMemberRole, removeMember } =
    useProjectMembers(projectId, 0, MEMBER_PREVIEW_SIZE);

  const currentUserRole = project?.currentUserRole ?? 'VIEWER';
  const canManageMembers = currentUserRole === 'ADMIN';
  const currentUserId = authStore.user?.id;
  const { createInvitation, isCreatingInvitation } = useProjectInvitations(
    projectId,
    { enabled: canManageMembers },
  );
  const currentMember = members.find(
    (member) => member.userId === currentUserId,
  );
  const orderedMembers = currentMember
    ? [
        currentMember,
        ...members.filter((member) => member.userId !== currentUserId),
      ]
    : members;
  const totalMembers = membersData?.totalElements ?? 0;

  const handleInvite = () => {
    if (!projectId || !email.trim()) return;
    createInvitation(
      { email, role: inviteRole },
      {
        onSuccess: () => {
          setEmail('');
          setInviteRole('VIEWER');
        },
      },
    );
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant={'none'}
          size={'none'}
          className="schemafy-menu-button schemafy-header-button flex h-9 w-9 items-center justify-center p-0 sm:h-auto sm:w-auto sm:px-3 sm:py-2"
          aria-label="Share"
        >
          <Share2 className="h-4 w-4 sm:hidden" />
          <span className="hidden sm:inline">Share</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex w-[calc(100vw-2rem)] min-w-0 flex-col gap-3 font-body-xs sm:w-auto sm:min-w-[24rem]"
      >
        <div className="flex flex-col gap-0.5">
          <span className="font-overline-xs text-schemafy-text">
            Share project
          </span>
          <span className="font-caption-sm text-schemafy-dark-gray">
            Manage project members and invitations.
          </span>
        </div>
        {canManageMembers && (
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-[minmax(0,1fr)_6.75rem_5.5rem] sm:items-center">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="schemafy-input h-10 min-w-0 px-3 py-0 font-body-xs"
              placeholder="schemafy@email.com"
            />
            <RoleSelect
              value={inviteRole}
              onValueChange={setInviteRole}
              userRole={currentUserRole}
              className="h-10 w-full"
            />
            <Button
              size={'dropdown'}
              className="h-10 w-full px-4"
              onClick={handleInvite}
              disabled={isCreatingInvitation}
            >
              Invite
            </Button>
          </div>
        )}
        <p className="font-overline-xs text-schemafy-dark-gray">
          Who has access
        </p>
        {orderedMembers.map((member) => (
          <div
            key={member.userId}
            className="flex items-center justify-between gap-1 rounded-xl px-2 py-1.5 hover:bg-schemafy-secondary"
          >
            <div className="flex min-w-0 items-center gap-2.5">
              <Avatar size={'dropdown'} />
              <p className="truncate">{member.userName}</p>
            </div>
            <div className="flex shrink-0 items-center gap-1">
              {canManageMembers && member.userId !== currentUserId ? (
                <>
                  <RoleSelect
                    value={member.role}
                    onValueChange={(role) =>
                      updateMemberRole(member.userId, { role })
                    }
                    userRole={currentUserRole}
                  />
                  <MemberRemoveButton
                    member={member}
                    onRemoveClick={setRemoveTarget}
                  />
                </>
              ) : (
                <RoleText role={member.role} />
              )}
            </div>
          </div>
        ))}
        {totalMembers > MEMBER_PREVIEW_SIZE && (
          <Button
            variant="none"
            size="none"
            className="mt-1 flex w-full items-center justify-center gap-2 rounded-xl px-2 py-2 font-caption-sm text-schemafy-soft-blue hover:bg-schemafy-secondary"
            onClick={() => setIsMembersDialogOpen(true)}
          >
            <Users className="h-4 w-4" />
            View all {totalMembers} members
          </Button>
        )}
        <ProjectMembersDialog
          projectId={projectId}
          open={isMembersDialogOpen}
          onOpenChange={setIsMembersDialogOpen}
        />

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
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
