import { Avatar } from '../Avatar';
import { Button } from '../Button';

export const LandingContents = () => {
  return (
    <div className="flex items-center gap-2">
      <Button round to="/signup">
        Get Started
      </Button>
      <Button variant={'secondary'} round to="/signin">
        Sign In
      </Button>
    </div>
  );
};

export const DashBoardContents = () => {
  return (
    <div className="flex items-center gap-9">
      <Button variant={'none'} size={'none'}>
        Projects
      </Button>
      <Button variant={'none'} size={'none'}>
        Settings
      </Button>
      <Button variant={'none'} size={'none'}>
        Notifications
      </Button>
      <div className="flex gap-2">
        <Button round>New Project</Button>
        <Button variant={'secondary'} round to="/signin">
          Sign Out
        </Button>
      </div>
      <Avatar src="https://picsum.photos/200/300?random=1" />
    </div>
  );
};

export const CanvasContents = () => {
  return (
    <div className="flex items-center gap-9">
      <Button variant={'none'} size={'none'}>
        Import
      </Button>
      <Button variant={'none'} size={'none'}>
        Export
      </Button>
      <Button variant={'none'} size={'none'}>
        Share
      </Button>
      <Button variant={'none'} size={'none'}>
        Versions
      </Button>
      <Button variant={'none'} size={'none'}>
        Settings
      </Button>
      <Button variant={'secondary'} round>
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
