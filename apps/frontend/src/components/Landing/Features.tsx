import { landingPageImages } from '@/assets';

const FEATURES = [
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

export const Features = () => {
  return (
    <section className="inline-flex flex-col px-4 py-10 gap-10">
      <div className="flex flex-col justify-between">
        <h2 className="font-display-md">Features</h2>
        <p className="font-body-md">
          Experience the benefits of using Schemafy for your ERD design needs.
        </p>
      </div>
      <div className="flex flex-grow gap-3">
        {FEATURES.map((feature) => (
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
