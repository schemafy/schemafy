import { useState } from 'react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuTrigger, } from '../../DropDown';
import { Button } from '../../Button';
import { Avatar } from '../../Avatar';
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue, } from '../../Select';
import {
  useCreateInvitation,
  useGetMembers,
  useGetProject,
  useUpdateMemberRole,
} from '@/features/project/hooks/useProjects';
import { availableRoles } from '@/features/project/utils/role';

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
              {role.toLowerCase()}
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
};

export const ShareContents = ({projectId}: { projectId: string }) => {
  const [email, setEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('VIEWER');

  const {data: projectData} = useGetProject(projectId);
  const {data: membersData} = useGetMembers(projectId);
  const {mutate: createInvitation, isPending} =
    useCreateInvitation(projectId);
  const {mutate: updateMemberRole} = useUpdateMemberRole(projectId);

  const currentUserRole = projectData?.currentUserRole ?? 'VIEWER';
  const members = membersData?.content ?? [];

  const handleInvite = () => {
    if (!projectId || !email.trim()) return;
    createInvitation(
      {email, role: inviteRole},
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
        {currentUserRole === 'ADMIN' && (
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
              disabled={isPending}
            >
              Invite
            </Button>
          </div>
        )}
        <p className="text-schemafy-dark-gray">Who has access</p>
        {members.map((member) => (
          <div
            key={member.userId}
            className="flex justify-between items-center"
          >
            <div className="flex gap-2.5 items-center">
              <Avatar size={'dropdown'}/>
              <p>{member.userName}</p>
            </div>
            <RoleSelect
              value={member.role}
              onValueChange={(role) =>
                updateMemberRole({userId: member.userId, data: {role}})
              }
              userRole={currentUserRole}
            />
          </div>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
