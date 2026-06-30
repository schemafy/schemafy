import { authStore } from '@/store/auth.store';
import { Button, ButtonLink } from '../Button';
import { Avatar } from '../Avatar';
import { NotificationContents } from './contents/NotificationContents';
import { useLogout } from '@/features/auth';

export const DashboardHeader = () => {
  const handleLogout = useLogout();

  return (
    <div className="flex items-center justify-end gap-5 w-full">
      <div className="flex items-center gap-9 ml-8">
        <ButtonLink to="/workspace" variant={'none'} size={'none'}>
          Projects
        </ButtonLink>
        <ButtonLink to="/settings" variant={'none'} size={'none'}>
          Settings
        </ButtonLink>
        <NotificationContents />
      </div>
      <div className="flex items-center gap-4">
        <div className="flex gap-2">
          <ButtonLink round to="/workspace">
            New Project
          </ButtonLink>
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
