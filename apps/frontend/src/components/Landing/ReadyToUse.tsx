import { Button } from '@/components';

export const ReadyToUse = () => {
  return (
    <section className="flex flex-col py-20 px-10 gap-8 items-center">
      <div className="flex flex-col gap-2 items-center">
        <h3 className="font-display-md">Ready to design your next database?</h3>
        <p className="font-body-md">
          Start using Schemafy today and experience the difference.
        </p>
      </div>
      <Button>Get Started</Button>
    </section>
  );
};
