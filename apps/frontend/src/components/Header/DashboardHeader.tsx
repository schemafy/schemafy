import { authStore } from '@/store/auth.store';
import { Button } from '../Button';
import { Avatar } from '../Avatar';
import { NotificationContents } from './contents/NotificationContents';
import { logout } from '@/features/auth/api';
import { useNavigate, useRouteContext } from '@tanstack/react-router';

export const DashboardHeader = () => {
  const navigate = useNavigate();
  const { queryClient } = useRouteContext({ from: '__root__' });

  const handleLogout = async () => {
    try {
      await logout();
      authStore.clearAuth();
      queryClient.clear();
      await navigate({ to: '/signin', search: { oauthError: null } });
    } catch (error) {
      console.error('Failed to sign out', error);
    }
  };

  return (
    <div className="flex items-center justify-end gap-5 w-full">
      <div className="flex items-center gap-9 ml-8">
        <Button to="/workspace" variant={'none'} size={'none'}>
          Projects
        </Button>
        <Button variant={'none'} size={'none'}>
          Settings
        </Button>
        <NotificationContents />
      </div>
      <div className="flex items-center gap-4">
        <div className="flex gap-2">
          <Button round to="/workspace">
            New Project
          </Button>
          <Button variant={'secondary'} round onClick={handleLogout}>
            Sign Out
          </Button>
        </div>
        <span className="flex items-center font-body-sm text-schemafy-dark-gray">
          {authStore.user?.name}
        </span>
        <Avatar src="https://picsum.photos/200/300?random=1" />
      </div>
    </div>
  );
};
