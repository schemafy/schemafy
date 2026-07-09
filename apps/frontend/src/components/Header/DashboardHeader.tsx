import { authStore } from '@/store/auth.store';
import { Button, ButtonLink } from '../Button';
import { Avatar } from '../Avatar';
import { NotificationContents } from './contents/NotificationContents';
import { useLogout } from '@/features/auth';
import { FolderKanban, LogOut, Plus, Settings } from 'lucide-react';

export const DashboardHeader = () => {
  const handleLogout = useLogout();

  return (
    <div className="flex w-full min-w-0 items-center justify-end">
      <div className="flex min-w-0 flex-nowrap items-center justify-end gap-1.5 pl-2 sm:min-w-max sm:gap-5">
        <ButtonLink
          to="/workspace"
          variant={'none'}
          size={'none'}
          className="schemafy-menu-button schemafy-header-button flex h-9 w-9 shrink-0 items-center justify-center p-0 sm:h-auto sm:w-auto sm:px-3 sm:py-2"
          aria-label="Projects"
        >
          <FolderKanban className="h-4 w-4 sm:hidden" />
          <span className="hidden sm:inline">Projects</span>
        </ButtonLink>
        <Button
          variant={'none'}
          size={'none'}
          className="schemafy-menu-button schemafy-header-button flex h-9 w-9 shrink-0 items-center justify-center p-0 sm:h-auto sm:w-auto sm:px-3 sm:py-2"
          aria-label="Settings"
        >
          <Settings className="h-4 w-4 sm:hidden" />
          <span className="hidden sm:inline">Settings</span>
        </Button>
        <NotificationContents />
        <ButtonLink
          round
          to="/workspace"
          className="schemafy-header-button h-9 w-9 shrink-0 px-0 sm:h-10 sm:w-auto sm:px-4"
          aria-label="New Project"
        >
          <Plus className="h-4 w-4 sm:hidden" />
          <span className="hidden sm:inline">New Project</span>
        </ButtonLink>
        <Button
          variant={'secondary'}
          round
          onClick={handleLogout}
          className="schemafy-header-button h-9 w-9 shrink-0 px-0 sm:h-10 sm:w-auto sm:px-4"
          aria-label="Sign Out"
        >
          <LogOut className="h-4 w-4 sm:hidden" />
          <span className="hidden sm:inline">Sign Out</span>
        </Button>
        <span className="hidden items-center font-body-sm text-schemafy-dark-gray lg:flex">
          {authStore.user?.name}
        </span>
        <Avatar
          className="hidden ring-2 ring-schemafy-glass-border sm:flex"
          src="https://picsum.photos/200/300?random=1"
        />
      </div>
    </div>
  );
};
