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
import { availableRoles } from '@/features/project/utils/role';
import { authStore } from '@/store';
import { toCapitalized } from "@/lib";

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
}: {
  value: string;
  onValueChange: (value: string) => void;
  userRole?: string;
}) => {
  const roles = availableRoles(userRole);

  return (
    <Select value={value} onValueChange={onValueChange}>
      <SelectTrigger className="w-[3.75rem] border-none font-body-xs">
        <SelectValue/>
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

  const { project } = useProject(projectId);
  const { members, updateMemberRole } = useProjectMembers(projectId);

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
        <Button variant={'none'} size={'none'}>
          Share
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex flex-col gap-2.5 font-body-xs"
      >
        {canManageMembers && (
          <div className="flex gap-4">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-[12.5rem] placeholder:text-schemafy-dark-gray bg-secondary rounded-[10px] px-3 py-2"
              placeholder="schemafy@email.com"
            />
            <RoleSelect
              value={inviteRole}
              onValueChange={setInviteRole}
              userRole={currentUserRole}
            />
            <Button
              size={'dropdown'}
              onClick={handleInvite}
              disabled={isCreatingInvitation}
            >
              Invite
            </Button>
          </div>
        )}
        <p className="text-schemafy-dark-gray">Who has access</p>
        {orderedMembers.map((member) => (
          <div
            key={member.userId}
            className="flex justify-between items-center"
          >
            <div className="flex gap-2.5 items-center">
              <Avatar size={'dropdown'}/>
              <p>{member.userName}</p>
            </div>
            {canManageMembers && member.userId !== currentUserId ? (
              <RoleSelect
                value={member.role}
                onValueChange={(role) =>
                  updateMemberRole(member.userId, { role })
                }
                userRole={currentUserRole}
              />
            ) : (
              <RoleText role={member.role}/>
            )}
          </div>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
