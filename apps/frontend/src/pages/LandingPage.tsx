import {
  Description,
  Features,
  Benefits,
  ReadyToUse,
} from '@/components/Landing';

export const LandingPage = () => {
  return (
    <div className="flex w-full flex-col px-4 sm:px-6 lg:px-8">
      <Description />
      <Features />
      <Benefits />
      <ReadyToUse />
    </div>
  );
};
