import { Avatar } from '../../Avatar';
import { Button } from '../../Button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import {
  useAcceptInvitation,
  useGetMyInvitations,
  useRejectInvitation,
} from '@/features/workspace/hooks/useWorkspaces';
import type { WorkspaceInvitationResponse } from '@/features/workspace/api';

export const NotificationContents = () => {
  const { data: invitations } = useGetMyInvitations(0, 10);
  const pendingInvitations = (invitations?.content ?? []).filter(
    (inv) => inv.status === 'PENDING',
  );

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <div className="flex gap-0.5 items-center">
          <Button variant={'none'} size={'none'}>
            Notifications
          </Button>
          <div className="rounded-full bg-schemafy-destructive w-5 h-5 flex items-center justify-center text-schemafy-button-text text-sm">
            {pendingInvitations.length}
          </div>
        </div>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex flex-col gap-2.5 font-body-xs"
      >
        {pendingInvitations.length === 0 ? (
          <p className="font-body-sm text-schemafy-dark-gray px-1">
            No new notifications
          </p>
        ) : (
          pendingInvitations.map((invitation) => (
            <NotificationItem key={invitation.id} invitation={invitation} />
          ))
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

const NotificationItem = ({
  invitation,
}: {
  invitation: WorkspaceInvitationResponse;
}) => {
  const { mutate: accept, isPending: isAccepting } = useAcceptInvitation();
  const { mutate: reject, isPending: isRejecting } = useRejectInvitation();

  return (
    <div className="flex gap-2.5 items-center">
      <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1" />
      <div className="max-w-[10rem] font-body-xs text-schemafy-text">
        {invitation.invitedBy} invited you to workspace as{' '}
        <span className="font-semibold">{invitation.invitedRole}</span>
      </div>
      <Button
        size={'sm'}
        disabled={isAccepting || isRejecting}
        onClick={() => accept(invitation.id)}
      >
        Accept
      </Button>
      <Button
        size={'sm'}
        variant={'secondary'}
        disabled={isAccepting || isRejecting}
        onClick={() => reject(invitation.id)}
      >
        Decline
      </Button>
    </div>
  );
};
