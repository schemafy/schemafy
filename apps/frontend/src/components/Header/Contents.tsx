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
          Sign In
        </Button>
      </div>
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
    </div>
  );
};
