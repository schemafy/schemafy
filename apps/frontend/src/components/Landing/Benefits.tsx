import {
  CheckCircle2,
  GitBranch,
  KeyRound,
  MessageSquare,
  ShieldCheck,
  Sparkles,
  UsersRound,
} from 'lucide-react';

const BENEFITS = [
  {
    icon: Sparkles,
    visual: 'diagram',
    label: 'Modeling',
    title: 'Canvas-first Editing',
    description:
      'Build readable table maps with direct table, column, and relationship controls.',
  },
  {
    icon: ShieldCheck,
    visual: 'quality',
    label: 'Details',
    title: 'Readable Structure',
    description:
      'Keep primary keys, foreign keys, nullable fields, and data types visible while you edit.',
  },
  {
    icon: UsersRound,
    visual: 'team',
    label: 'Review',
    title: 'Project Sharing',
    description:
      'Use workspace roles, project sharing, and memos to keep design context in one place.',
  },
] as const;

type BenefitVisual = (typeof BENEFITS)[number]['visual'];

const DiagramPreview = () => (
  <div className="relative h-44 overflow-hidden rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/45 p-4">
    <div className="absolute inset-0 bg-[linear-gradient(90deg,hsl(var(--schemafy-light-gray)/0.22)_1px,transparent_1px),linear-gradient(180deg,hsl(var(--schemafy-light-gray)/0.22)_1px,transparent_1px)] bg-[size:24px_24px]" />
    <div className="relative grid h-full grid-cols-[1fr_2.5rem_1fr] items-center gap-2">
      <div className="rounded-xl border border-schemafy-glass-border bg-schemafy-panel p-3">
        <div className="mb-2 flex items-center gap-2">
          <KeyRound className="h-3.5 w-3.5 text-schemafy-yellow" />
          <span className="font-caption-sm text-schemafy-text">users</span>
        </div>
        <div className="space-y-1.5">
          <span className="block h-1.5 w-4/5 rounded-full bg-schemafy-dark-gray/45" />
          <span className="block h-1.5 w-3/5 rounded-full bg-schemafy-dark-gray/30" />
          <span className="block h-1.5 w-2/3 rounded-full bg-schemafy-dark-gray/30" />
        </div>
      </div>
      <div className="flex items-center">
        <span className="h-px flex-1 bg-schemafy-soft-blue/60" />
        <span className="h-2 w-2 rounded-full border border-schemafy-soft-blue bg-schemafy-panel-strong" />
        <span className="h-px flex-1 bg-schemafy-soft-blue/60" />
      </div>
      <div className="rounded-xl border border-schemafy-glass-border bg-schemafy-panel p-3">
        <div className="mb-2 flex items-center gap-2">
          <GitBranch className="h-3.5 w-3.5 text-schemafy-soft-blue" />
          <span className="font-caption-sm text-schemafy-text">projects</span>
        </div>
        <div className="space-y-1.5">
          <span className="block h-1.5 w-3/4 rounded-full bg-schemafy-dark-gray/45" />
          <span className="block h-1.5 w-1/2 rounded-full bg-schemafy-dark-gray/30" />
          <span className="block h-1.5 w-4/5 rounded-full bg-schemafy-dark-gray/30" />
        </div>
      </div>
    </div>
  </div>
);

const QualityPreview = () => {
  const rows = [
    ['Primary keys', 'Visible'],
    ['Foreign keys', 'Linked'],
    ['Nullable fields', 'Shown'],
  ];

  return (
    <div className="h-44 rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/45 p-4">
      <div className="flex h-full flex-col justify-between gap-2">
        <div className="grid gap-1.5">
          {rows.map(([label, value]) => (
            <div
              key={label}
              className="flex items-center justify-between gap-3 rounded-lg border border-schemafy-glass-border bg-schemafy-panel px-3 py-1.5"
            >
              <div className="flex min-w-0 items-center gap-2">
                <CheckCircle2 className="h-4 w-4 shrink-0 text-schemafy-green" />
                <span className="truncate font-caption-md text-schemafy-text">
                  {label}
                </span>
              </div>
              <span className="shrink-0 rounded-full bg-schemafy-green/12 px-2 py-0.5 font-caption-sm text-schemafy-green">
                {value}
              </span>
            </div>
          ))}
        </div>
        <div className="grid grid-cols-3 gap-2">
          {['PK', 'FK', 'NN'].map((item) => (
            <span
              key={item}
              className="rounded-lg border border-schemafy-glass-border bg-schemafy-panel px-2 py-1 text-center font-code-xs text-schemafy-dark-gray"
            >
              {item}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
};

const TeamPreview = () => (
  <div className="h-44 rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/45 p-4">
    <div className="flex h-full flex-col justify-between gap-3">
      <div className="flex items-center justify-between gap-3">
        <div className="flex -space-x-2">
          {['AD', 'ME', 'VI'].map((name) => (
            <span
              key={name}
              className="flex h-8 w-8 items-center justify-center rounded-full border border-schemafy-glass-border bg-schemafy-panel-strong font-caption-sm text-schemafy-text"
            >
              {name}
            </span>
          ))}
        </div>
        <span className="schemafy-badge px-2.5 py-1 font-caption-sm">
          Shared
        </span>
      </div>
      <div className="grid gap-2">
        {['users.email noted', 'orders.user_id linked'].map((message) => (
          <div
            key={message}
            className="flex items-center gap-2 rounded-lg border border-schemafy-glass-border bg-schemafy-panel px-3 py-2"
          >
            <MessageSquare className="h-4 w-4 shrink-0 text-schemafy-violet" />
            <span className="truncate font-caption-md text-schemafy-text">
              {message}
            </span>
          </div>
        ))}
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-schemafy-light-gray/55">
        <span className="block h-full w-2/3 rounded-full bg-schemafy-soft-blue/75" />
      </div>
    </div>
  </div>
);

const BenefitVisualPanel = ({ type }: { type: BenefitVisual }) => {
  if (type === 'quality') {
    return <QualityPreview />;
  }

  if (type === 'team') {
    return <TeamPreview />;
  }

  return <DiagramPreview />;
};

export const Benefits = () => {
  return (
    <section className="mx-auto flex w-full max-w-7xl flex-col gap-6 py-10">
      <div className="flex max-w-2xl flex-col gap-3">
        <span className="schemafy-badge w-fit px-3 py-1 font-caption-md">
          Design system in practice
        </span>
        <h2 className="font-heading-xl text-schemafy-text">
          Calm by default, detailed when needed
        </h2>
        <p className="font-body-md text-schemafy-dark-gray">
          Schemafy keeps the lower-level database details legible without making
          the workspace feel noisy.
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        {BENEFITS.map((benefit) => {
          const Icon = benefit.icon;

          return (
            <article
              key={benefit.title}
              className="schemafy-subtle-card flex min-h-[23rem] flex-col gap-4 p-4"
            >
              <BenefitVisualPanel type={benefit.visual} />
              <div className="flex flex-1 flex-col justify-between gap-4 px-1 pb-1">
                <div className="flex flex-col gap-2.5">
                  <div className="flex items-center gap-2 text-schemafy-dark-gray">
                    <Icon className="h-4 w-4" />
                    <span className="font-caption-md">{benefit.label}</span>
                  </div>
                  <h3 className="font-heading-sm text-schemafy-text">
                    {benefit.title}
                  </h3>
                  <p className="font-body-sm text-schemafy-dark-gray">
                    {benefit.description}
                  </p>
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
};
