import { Button } from '../Button';

export const DefaultContents = () => {
  return (
    <div className="flex items-center gap-9">
      <Button variant={'none'} size={'none'} to="/product">
        Product
      </Button>
      <Button variant={'none'} size={'none'} to="/pricing">
        Pricing
      </Button>
      <Button variant={'none'} size={'none'} to="/resources">
        Resources
      </Button>
      <div className="flex gap-2">
        <Button round to="/start">
          Get Started
        </Button>
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
