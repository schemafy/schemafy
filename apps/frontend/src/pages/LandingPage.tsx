import { landingPageImages } from '@/assets';
import { Button } from '@/components';

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

const Description = () => {
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

const Features = () => {
  const features = [
    {
      icon: landingPageImages.features.aiAssistant,
      title: 'AI Assistance',
      description:
        'Get AI-powered suggestions for your ERDs, making the design process faster and more intuitive.',
    },
    {
      icon: landingPageImages.features.collaboration,
      title: 'Collaboration',
      description:
        'Collaborate with your team in real-time, ensuring everyone is on the same page.',
    },
    {
      icon: landingPageImages.features.use,
      title: 'Easy to Use',
      description:
        "Schemafy's intuitive interface makes it easy to create and manage your ERDs, even without prior experience.",
    },
  ];

  return (
    <section className="inline-flex flex-col px-4 py-10 gap-10">
      <div className="flex flex-col justify-between">
        <h2 className="font-display-md">Benefits</h2>
        <p className="font-body-md">
          Experience the benefits of using Schemafy for your ERD design needs.
        </p>
      </div>
      <div className="flex flex-grow gap-3">
        {features.map((feature) => (
          <article
            key={feature.title}
            className="flex flex-col rounded-lg border border-schemafy-light-gray gap-3 w-[300px] p-4"
          >
            <img src={feature.icon} alt={feature.title} className="w-6 h-6" />
            <div className="flex flex-col gap-2">
              <h3 className="font-heading-sm">{feature.title}</h3>
              <p className="font-body-sm">{feature.description}</p>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
};

const Benefits = () => {
  const benefits = [
    {
      image: landingPageImages.benefits.faster,
      title: 'Faster Design',
      description:
        'Design ERDs in minutes with AI assistance, saving you valuable time.',
    },
    {
      image: landingPageImages.benefits.accuracy,
      title: 'Improved Accuracy',
      description:
        'Reduce errors and ensure accuracy with AI-powered suggestions and validation.',
    },
    {
      image: landingPageImages.benefits.teamwork,
      title: 'Enhanced Teamwork',
      description:
        'Work together seamlessly with real-time collaboration features, boosting team productivity.',
    },
  ];

  return (
    <section className="inline-flex flex-col px-4 py-10 gap-10">
      <div className="flex flex-col justify-between">
        <h2 className="font-display-md">Benefits</h2>
        <p className="font-body-md">
          Schemafy offers a range of features to help you design ERDs more
          efficiently.
        </p>
      </div>
      <div className="flex flex-grow gap-3">
        {benefits.map((benefit) => (
          <article
            key={benefit.title}
            className="flex flex-col gap-3 w-[300px]"
          >
            <img
              src={benefit.image}
              alt={benefit.title}
              className="self-stretch"
            />
            <div className="flex flex-col">
              <h3 className="font-heading-sm">{benefit.title}</h3>
              <p className="font-body-sm">{benefit.description}</p>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
};

const ReadyToUse = () => {
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
