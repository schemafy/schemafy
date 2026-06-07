import { Button } from '../Button';
import { Avatar } from '../Avatar';
import { ExportContents } from './contents/ExportContents';
import { ShareContents } from './contents/ShareContents';
import { SettingsContents } from './contents/SettingsContents';
import { useLogout } from '@/features/auth';
import { useParams } from '@tanstack/react-router';

export const CanvasHeader = () => {
  const { projectId } = useParams({ from: '/project/$projectId' });
  const handleLogout = useLogout();

  return (
    <div className="flex items-center gap-9">
      <ExportContents />
      <ShareContents projectId={projectId} />
      <SettingsContents />
      <Button variant={'secondary'} round onClick={handleLogout}>
        Sign Out
      </Button>
      <div className="flex items-center gap-2">
        <div className="flex items-center -space-x-3 *:data-[slot=avatar]:ring-background *:data-[slot=avatar]:ring-2 [&>*:nth-child(1)]:z-30 [&>*:nth-child(2)]:z-20 [&>*:nth-child(3)]:z-10">
          <Avatar src="https://picsum.photos/200/300?random=1" />
          <Avatar src="https://picsum.photos/100/300?random=1" deactivate />
          <Avatar src="https://picsum.photos/200/100?random=1" deactivate />
        </div>
        <span className="font-overline-xs text-schemafy-dark-gray">+ 3</span>
      </div>
    </div>
  );
};
