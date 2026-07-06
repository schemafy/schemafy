import { Button } from '../Button';
import { Avatar } from '../Avatar';
import { ExportContents } from './contents/ExportContents';
import { ShareContents } from './contents/ShareContents';
import { SettingsContents } from './contents/SettingsContents';
import { useLogout } from '@/features/auth';
import { useParams } from '@tanstack/react-router';
import { LogOut } from 'lucide-react';

export const CanvasHeader = () => {
  const params = useParams({
    strict: false,
    shouldThrow: false,
  });
  const projectId = params?.projectId ?? '';
  const handleLogout = useLogout();

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
      <div className="hidden items-center gap-2 sm:flex">
        <div className="flex items-center -space-x-3 *:data-[slot=avatar]:ring-2 *:data-[slot=avatar]:ring-schemafy-bg [&>*:nth-child(1)]:z-30 [&>*:nth-child(2)]:z-20 [&>*:nth-child(3)]:z-10">
          <Avatar src="https://picsum.photos/200/300?random=1" />
          <Avatar src="https://picsum.photos/100/300?random=1" deactivate />
          <Avatar src="https://picsum.photos/200/100?random=1" deactivate />
        </div>
        <span className="font-overline-xs text-schemafy-dark-gray">+ 3</span>
      </div>
    </div>
  );
};
