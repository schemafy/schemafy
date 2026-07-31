import { landingPageImages } from '@/assets';

const FEATURES = [
  {
    icon: landingPageImages.features.aiAssistant,
    title: 'Canvas Modeling',
    description:
      'Create tables, columns, relationships, indexes, and constraints directly on the ERD canvas.',
  },
  {
    icon: landingPageImages.features.collaboration,
    title: 'Collaboration',
    description:
      'Share projects with workspace members and discuss schema decisions with memos.',
  },
  {
    icon: landingPageImages.features.use,
    title: 'Easy to Use',
    description:
      'Keep project, workspace, and schema editing controls close to the task at hand.',
  },
];

export const Features = () => {
  return (
    <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 py-10">
      <div className="flex max-w-2xl flex-col gap-3">
        <span className="schemafy-badge w-fit px-3 py-1 font-caption-md">
          Product workflow
        </span>
        <h2 className="font-heading-xl text-schemafy-text">
          Designed for ERD work
        </h2>
        <p className="font-body-md text-schemafy-dark-gray">
          Every surface stays close to the modeling task, from entity structure
          to review feedback.
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        {FEATURES.map((feature, index) => (
          <article
            key={feature.title}
            className="schemafy-subtle-card flex min-h-44 flex-col justify-between gap-5 p-5"
          >
            <div className="flex items-center justify-between gap-4">
              <div className="flex h-11 w-11 items-center justify-center rounded-xl border border-schemafy-glass-border bg-schemafy-secondary">
                <img
                  src={feature.icon}
                  alt={feature.title}
                  className="h-5 w-5"
                />
              </div>
              <span className="font-code-xs text-schemafy-dark-gray">
                0{index + 1}
              </span>
            </div>
            <div className="flex flex-col gap-2.5">
              <h3 className="font-heading-sm text-schemafy-text">
                {feature.title}
              </h3>
              <p className="font-body-sm text-schemafy-dark-gray">
                {feature.description}
              </p>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
};
