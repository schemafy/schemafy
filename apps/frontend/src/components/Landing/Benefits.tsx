import { landingPageImages } from '@/assets';

const BENEFITS = [
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

export const Benefits = () => {
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
        {BENEFITS.map((benefit) => (
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
