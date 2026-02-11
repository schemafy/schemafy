import { landingPageImages } from '@/assets';
import { Button } from '@/components';

export const Description = () => {
  return (
    <section className="self-stretch flex px-4 py-10 gap-8 flex-grow">
      <img src={landingPageImages.hero.description} alt="description image" />
      <div className="inline-flex flex-col justify-center items-start gap-8">
        <div className="flex flex-col justify-start items-start gap-2">
          <h2 className="font-display-lg">
            Design ERDs with AI assistance and collaboration features
          </h2>
          <p className="font-body-md">
            Schemafy is a tool for drawing ERDs with AI assistance and
            collaboration features. It helps you design databases faster and
            more efficiently.
          </p>
        </div>
        <Button fullWidth>Get Started</Button>
      </div>
    </section>
  );
};
