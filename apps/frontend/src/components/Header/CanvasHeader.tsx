import { Button } from '../Button';
import { Avatar } from '../Avatar';
import { getRandomColor } from '@/features/collaboration/utils';
import { ExportContents } from './contents/ExportContents';
import { ShareContents } from './contents/ShareContents';
import { SettingsContents } from './contents/SettingsContents';
import { useLogout } from '@/features/auth';
import { useParams } from '@tanstack/react-router';
import { LogOut } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { collaborationStore } from '@/store/collaboration.store';

const MAX_VISIBLE_AVATARS = 4;

export const CanvasHeader = observer(() => {
  const params = useParams({
    strict: false,
    shouldThrow: false,
  });
  const projectId = params?.projectId ?? '';
  const handleLogout = useLogout();

  const participants = collaborationStore.activeParticipants;
  const visibleParticipants = participants.slice(0, MAX_VISIBLE_AVATARS);
  const overflowCount = participants.length - MAX_VISIBLE_AVATARS;

  return (
    <div className="flex min-w-0 items-center gap-1 sm:gap-4">
      <ExportContents />
      <ShareContents projectId={projectId} />
      <SettingsContents />
      <Button
        variant={'secondary'}
        round
        onClick={handleLogout}
        className="schemafy-header-button h-9 w-9 border border-schemafy-glass-border bg-schemafy-panel px-0 text-schemafy-text shadow-sm hover:bg-schemafy-secondary sm:h-10 sm:w-auto sm:px-4"
        aria-label="Sign Out"
      >
        <LogOut className="h-4 w-4 sm:hidden" />
        <span className="hidden sm:inline">Sign Out</span>
      </Button>
      {participants.length > 0 && (
        <div className="hidden items-center gap-2 sm:flex">
          <div className="flex items-center -space-x-3 *:data-[slot=avatar]:ring-2 *:data-[slot=avatar]:ring-schemafy-bg [&>*:nth-child(1)]:z-30 [&>*:nth-child(2)]:z-20 [&>*:nth-child(3)]:z-10 [&>*:nth-child(4)]:z-0">
            {visibleParticipants.map((p) => (
              <Avatar
                key={p.sessionId}
                src={p.profileImageUrl ?? undefined}
                name={p.userName}
                color={getRandomColor(p.userId)}
              />
            ))}
          </div>
          {overflowCount > 0 && (
            <span className="font-overline-xs text-schemafy-dark-gray">
              +{overflowCount}
            </span>
          )}
        </div>
      )}
    </div>
  );
});
