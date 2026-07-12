import { Avatar } from '../../Avatar';
import { Button } from '../../Button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { useMyWorkspaceInvitations } from '@/features/workspace/hooks/useMyWorkspaceInvitations';
import { useMyProjectInvitations } from '@/features/project/hooks/useMyProjectInvitations';
import { cn } from '@/lib';
import { Bell } from 'lucide-react';

type UnifiedInvitation =
  | {
      type: 'workspace';
      id: string;
      invitedBy: string;
      targetName: string;
      invitedRole: string;
      createdAt: string;
    }
  | {
      type: 'project';
      id: string;
      invitedBy: string;
      targetName: string;
      invitedRole: string;
      createdAt: string;
    };

export const NotificationContents = () => {
  const {
    myWorkspaceInvitations,
    acceptWorkspaceInvitation,
    rejectWorkspaceInvitation,
    isAcceptingWorkspaceInvitation,
    isRejectingWorkspaceInvitation,
  } = useMyWorkspaceInvitations(0, 20);
  const {
    myProjectInvitations,
    acceptProjectInvitation,
    rejectProjectInvitation,
    isAcceptingProjectInvitation,
    isRejectingProjectInvitation,
  } = useMyProjectInvitations(0, 20);

  const isPending =
    isAcceptingWorkspaceInvitation ||
    isRejectingWorkspaceInvitation ||
    isAcceptingProjectInvitation ||
    isRejectingProjectInvitation;

  const unified: UnifiedInvitation[] = [
    ...myWorkspaceInvitations
      .filter((inv) => inv.status === 'PENDING')
      .map((inv) => ({
        type: 'workspace' as const,
        id: inv.id,
        invitedBy: inv.invitedBy,
        targetName: inv.targetName,
        invitedRole: inv.invitedRole,
        createdAt: inv.createdAt,
      })),
    ...myProjectInvitations
      .filter((inv) => inv.status === 'PENDING')
      .map((inv) => ({
        type: 'project' as const,
        id: inv.id,
        invitedBy: inv.invitedBy,
        targetName: inv.targetName,
        invitedRole: inv.invitedRole,
        createdAt: inv.createdAt,
      })),
  ].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  );

  const handleAccept = (invitation: UnifiedInvitation) => {
    if (invitation.type === 'workspace') {
      acceptWorkspaceInvitation(invitation.id);
    } else {
      acceptProjectInvitation(invitation.id);
    }
  };

  const handleReject = (invitation: UnifiedInvitation) => {
    if (invitation.type === 'workspace') {
      rejectWorkspaceInvitation(invitation.id);
    } else {
      rejectProjectInvitation(invitation.id);
    }
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <div
          className="schemafy-menu-button schemafy-header-button relative flex h-9 w-9 items-center justify-center p-0 sm:h-auto sm:w-auto sm:gap-1 sm:px-3 sm:py-2"
          aria-label="Notifications"
        >
          <Bell className="h-4 w-4 sm:hidden" />
          <Button
            variant={'none'}
            size={'none'}
            className="pointer-events-none hidden sm:inline-flex"
          >
            Notifications
          </Button>
          <div
            className={cn(
              'flex h-5 w-5 items-center justify-center rounded-full text-sm sm:static',
              'absolute -right-1 -top-1',
              unified.length > 0
                ? 'bg-schemafy-destructive text-white'
                : 'schemafy-badge text-schemafy-dark-gray',
            )}
          >
            {unified.length}
          </div>
        </div>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex w-[calc(100vw-2rem)] min-w-0 flex-col gap-3 font-body-xs sm:w-auto sm:min-w-[22rem]"
      >
        <div className="flex flex-col gap-0.5">
          <span className="font-overline-xs text-schemafy-text">
            Notifications
          </span>
          <span className="font-caption-sm text-schemafy-dark-gray">
            Workspace and project invitations.
          </span>
        </div>
        {unified.length === 0 ? (
          <p className="rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/50 px-3 py-4 text-center font-body-sm text-schemafy-dark-gray">
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

const NotificationItem = ({
  invitation,
  isPending,
  onAccept,
  onReject,
}: NotificationItemProps) => {
  const invitedBy = invitation.invitedBy || 'Unknown';
  const targetTypeLabel =
    invitation.type === 'workspace' ? 'Workspace' : 'Project';

  return (
    <div className="flex items-center gap-3 rounded-xl border border-schemafy-glass-border bg-schemafy-panel px-3 py-2">
      <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1" />
      <div className="min-w-0 max-w-[11.5rem] font-body-xs text-schemafy-text">
        <div className="break-words">
          {invitedBy} invited you to {targetTypeLabel}{' '}
          <span className="break-words font-semibold">
            {invitation.targetName}
          </span>{' '}
          as <span className="font-semibold">{invitation.invitedRole}</span>
        </div>
      </div>
      <Button
        size={'sm'}
        disabled={isPending}
        onClick={() => onAccept(invitation)}
      >
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
