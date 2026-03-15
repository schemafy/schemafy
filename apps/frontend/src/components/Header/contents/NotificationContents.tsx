import { Avatar } from '../../Avatar';
import { Button } from '../../Button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuTrigger, } from '../../DropDown';
import {
  useAcceptWorkspaceInvitation,
  useGetMyWorkspaceInvitations,
  useRejectWorkspaceInvitation,
} from '@/features/workspace/hooks/useWorkspaces';
import {
  useAcceptProjectInvitation,
  useGetMyProjectInvitations,
  useRejectProjectInvitation,
} from '@/features/project/hooks/useProjects';

type UnifiedInvitation =
  | { type: 'workspace'; id: string; invitedBy: string; invitedRole: string; createdAt: string }
  | { type: 'project'; id: string; invitedBy: string; invitedRole: string; createdAt: string };

export const NotificationContents = () => {
  const {data: workspaceInvitations} = useGetMyWorkspaceInvitations(0, 20);
  const {data: projectInvitations} = useGetMyProjectInvitations(0, 20);
  const {mutate: acceptWorkspace, isPending: isAcceptingWorkspace} = useAcceptWorkspaceInvitation();
  const {mutate: rejectWorkspace, isPending: isRejectingWorkspace} = useRejectWorkspaceInvitation();
  const {mutate: acceptProject, isPending: isAcceptingProject} = useAcceptProjectInvitation();
  const {mutate: rejectProject, isPending: isRejectingProject} = useRejectProjectInvitation();

  const isPending = isAcceptingWorkspace || isRejectingWorkspace || isAcceptingProject || isRejectingProject;

  const unified: UnifiedInvitation[] = [
    ...(workspaceInvitations?.content ?? [])
      .filter((inv) => inv.status === 'PENDING')
      .map((inv) => ({
        type: 'workspace' as const,
        id: inv.id,
        invitedBy: inv.invitedBy,
        invitedRole: inv.invitedRole,
        createdAt: inv.createdAt
      })),
    ...(projectInvitations?.content ?? [])
      .filter((inv) => inv.status === 'PENDING')
      .map((inv) => ({
        type: 'project' as const,
        id: inv.id,
        invitedBy: inv.invitedBy,
        invitedRole: inv.invitedRole,
        createdAt: inv.createdAt
      })),
  ].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  const handleAccept = (invitation: UnifiedInvitation) => {
    if (invitation.type === 'workspace') acceptWorkspace(invitation.id);
    else acceptProject(invitation.id);
  };

  const handleReject = (invitation: UnifiedInvitation) => {
    if (invitation.type === 'workspace') rejectWorkspace(invitation.id);
    else rejectProject(invitation.id);
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <div className="flex gap-0.5 items-center">
          <Button variant={'none'} size={'none'}>
            Notifications
          </Button>
          <div
            className="rounded-full bg-schemafy-destructive w-5 h-5 flex items-center justify-center text-schemafy-button-text text-sm">
            {unified.length}
          </div>
        </div>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex flex-col gap-2.5 font-body-xs"
      >
        {unified.length === 0 ? (
          <p className="font-body-sm text-schemafy-dark-gray px-1">
            No new notifications
          </p>
        ) : (
          unified.map((invitation) => (
            <NotificationItem
              key={`${invitation.type}-${invitation.id}`}
              invitation={invitation}
              isPending={isPending}
              onAccept={handleAccept}
              onReject={handleReject}
            />
          ))
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

type NotificationItemProps = {
  invitation: UnifiedInvitation;
  isPending: boolean;
  onAccept: (invitation: UnifiedInvitation) => void;
  onReject: (invitation: UnifiedInvitation) => void;
};

const NotificationItem = ({invitation, isPending, onAccept, onReject}: NotificationItemProps) => {
  return (
    <div className="flex gap-2.5 items-center">
      <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1"/>
      <div className="max-w-[10rem] font-body-xs text-schemafy-text">
        {invitation.invitedBy} invited you to{' '}
        <span className="font-semibold">{invitation.type}</span> as{' '}
        <span className="font-semibold">{invitation.invitedRole}</span>
      </div>
      <Button size={'sm'} disabled={isPending} onClick={() => onAccept(invitation)}>
        Accept
      </Button>
      <Button
        size={'sm'}
        variant={'secondary'}
        disabled={isPending}
        onClick={() => onReject(invitation)}
      >
        Decline
      </Button>
    </div>
  );
};