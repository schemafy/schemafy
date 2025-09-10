import { Button } from '../Button';
import { Link } from 'react-router-dom';

export const DefaultContents = () => {
  return (
    <div className="flex items-center gap-9">
      <Link to="/product">
        <Button variant={'none'} size={'none'}>
          Product
        </Button>
      </Link>
      <Link to="/pricing">
        <Button variant={'none'} size={'none'}>
          Pricing
        </Button>
      </Link>
      <Link to="/resources">
        <Button variant={'none'} size={'none'}>
          Resources
        </Button>
      </Link>
      <div className="flex gap-2">
        <Link to="/start">
          <Button round>Get Started</Button>
        </Link>
        <Link to="/signin">
          <Button variant={'secondary'} round>
            Sign In
          </Button>
        </Link>
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
