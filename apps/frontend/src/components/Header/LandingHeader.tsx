import { ButtonLink } from '../Button';

export const LandingHeader = () => {
  return (
    <div className="flex items-center gap-2">
      <ButtonLink round to="/signup">
        Get Started
      </ButtonLink>
      <ButtonLink variant={'secondary'} round to="/signin">
        Sign In
      </ButtonLink>
    </div>
  );
};
