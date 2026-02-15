import {
  Description,
  Features,
  Benefits,
  ReadyToUse,
} from '@/components/Landing';

export const LandingPage = () => {
  return (
    <div className="flex-col">
      <Description />
      <Features />
      <Benefits />
      <ReadyToUse />
    </div>
  );
};
