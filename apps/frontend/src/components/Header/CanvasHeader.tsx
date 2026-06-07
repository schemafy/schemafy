import { Button } from '../Button';
import { Avatar } from '../Avatar';
import { ExportContents } from './contents/ExportContents';
import { ShareContents } from './contents/ShareContents';
import { SettingsContents } from './contents/SettingsContents';
import { logout } from '@/features/auth/api';
import { clearAuthSession } from '@/features/auth/lib/auth-session';
import { useNavigate, useParams } from '@tanstack/react-router';
import { toast } from 'sonner';

export const CanvasHeader = () => {
  const { projectId } = useParams({ from: '/project/$projectId' });
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logout();
      clearAuthSession();
      await navigate({ to: '/signin', search: { oauthError: null } });
    } catch {
      toast.error('Failed to sign out. Please try again.');
    }
  };

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
