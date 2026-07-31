import { ButtonLink } from '../Button';

export const LandingHeader = () => {
  return (
    <div className="flex min-w-0 items-center gap-2">
      <ButtonLink round to="/signup" className="px-4">
        Get Started
      </ButtonLink>
      <ButtonLink variant={'secondary'} round to="/signin" className="px-4">
        Sign In
      </ButtonLink>
    </div>
  );
};
