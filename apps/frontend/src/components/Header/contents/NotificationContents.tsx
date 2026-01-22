import { Avatar } from '../../Avatar';
import { Button } from '../../Button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';

interface InviteType {
  inviteUser: string;
  targetName: string;
  role: string;
  inviteType: 'project' | 'workspace';
}

export const NotificationContents = () => {
  const inviteMessages: InviteType[] = [
    {
      inviteUser: 'user1',
      targetName: 'Alpha',
      role: 'admin',
      inviteType: 'project',
    },
    {
      inviteUser: 'user2',
      targetName: 'Beta',
      role: 'viewer',
      inviteType: 'workspace',
    },
    {
      inviteUser: 'user3',
      targetName: 'Gamma',
      role: 'editor',
      inviteType: 'project',
    },
  ];

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <div className="flex gap-0.5 items-center">
          <Button variant={'none'} size={'none'}>
            Notifications
          </Button>
          <div className="rounded-full bg-schemafy-destructive w-5 h-5 flex items-center justify-center text-schemafy-button-text text-sm">
            {inviteMessages.length}
          </div>
        </div>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex flex-col gap-2.5 font-body-xs"
      >
        {inviteMessages.map((message, index) => (
          <NotificationItem key={index} invite={message} />
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

const NotificationItem = ({ invite }: { invite: InviteType }) => {
  return (
    <div className="flex gap-2.5 items-center">
      <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1" />
      <div className="max-w-[10rem] font-body-xs text-schemafy-text">
        {invite.inviteUser} invited you to {invite.targetName}{' '}
        {invite.inviteType}
      </div>
      <Button size={'sm'} className="">
        Accept
      </Button>
      <Button size={'sm'} variant={'secondary'}>
        Decline
      </Button>
    </div>
  );
};
