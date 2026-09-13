import { CheckCircle2, Database, GitBranch, KeyRound } from 'lucide-react';
import { landingPageImages } from '@/assets';
import { Button } from '@/components';

const HERO_METRICS = [
  ['12', 'tables mapped'],
  ['28', 'relations traced'],
  ['4', 'memos attached'],
] as const;

const HERO_PREVIEW_ROWS = [
  ['users.id', KeyRound, 'PK'],
  ['projects.owner_id', GitBranch, 'FK'],
  ['memo.comment', CheckCircle2, 'Note'],
] as const;

export const Description = () => {
  return (
    <section className="relative my-6 flex min-h-[min(680px,calc(100vh-7rem))] overflow-hidden rounded-[1.5rem] border border-schemafy-glass-border shadow-[0_28px_80px_-56px_rgb(15_23_42/0.8)]">
      <div className="absolute inset-0">
        <img
          src={landingPageImages.hero.description}
          alt="Schemafy ERD canvas preview"
          className="h-full w-full object-cover object-left-top"
        />
        <div className="absolute inset-0 bg-[linear-gradient(90deg,hsl(var(--schemafy-bg))_0%,hsl(var(--schemafy-bg)/0.92)_38%,hsl(var(--schemafy-bg)/0.42)_72%,transparent_100%)]" />
        <div className="absolute inset-0 bg-[linear-gradient(180deg,transparent_0%,hsl(var(--schemafy-canvas)/0.34)_100%)]" />
      </div>
      <div className="relative z-10 flex w-full flex-col justify-center gap-8 px-6 py-12 sm:px-10 lg:flex-row lg:items-center lg:justify-between lg:px-14">
        <div className="flex max-w-3xl flex-col items-start gap-7">
          <div className="flex flex-col items-start gap-4">
            <span className="schemafy-badge px-3 py-1 font-caption-md">
              Modern ERD design workspace
            </span>
            <h2 className="font-display-lg text-schemafy-text">Schemafy</h2>
            <p className="max-w-2xl font-body-md text-schemafy-dark-gray">
              Design ERDs with precision, collaboration, and less visual noise.
              Keep database structure, schema decisions, and team feedback in
              one calm canvas.
            </p>
          </div>
          <div className="grid w-full max-w-xl gap-3 sm:grid-cols-3">
            {HERO_METRICS.map(([value, label]) => (
              <div
                key={label}
                className="rounded-2xl border border-schemafy-glass-border bg-schemafy-panel/70 px-4 py-3 backdrop-blur-xl"
              >
                <p className="font-heading-md text-schemafy-text">{value}</p>
                <p className="font-caption-md text-schemafy-dark-gray">
                  {label}
                </p>
              </div>
            ))}
          </div>
          <Button className="px-6" round>
            Get Started
          </Button>
        </div>
        <HeroSchemaPreview />
      </div>
    </section>
  );
};

const HeroSchemaPreview = () => (
  <div className="hidden w-full max-w-md shrink-0 lg:block">
    <div className="schemafy-strong-panel rounded-2xl p-4">
      <div className="mb-4 flex items-center justify-between gap-4 border-b border-schemafy-glass-border pb-3">
        <div className="flex items-center gap-2">
          <Database className="h-4 w-4 text-schemafy-soft-blue" />
          <span className="font-heading-xs text-schemafy-text">
            Database map
          </span>
        </div>
        <span className="rounded-full bg-schemafy-green/12 px-2.5 py-1 font-caption-sm text-schemafy-green">
          Ready
        </span>
      </div>
      <div className="grid gap-3">
        {HERO_PREVIEW_ROWS.map(([label, Icon, badge]) => (
          <div
            key={label}
            className="flex items-center justify-between gap-4 rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/55 px-3 py-2.5"
          >
            <div className="flex min-w-0 items-center gap-2">
              <Icon className="h-4 w-4 shrink-0 text-schemafy-soft-blue" />
              <span className="truncate font-caption-md text-schemafy-text">
                {label}
              </span>
            </div>
            <span className="rounded-full border border-schemafy-glass-border bg-schemafy-panel px-2 py-0.5 font-code-xs text-schemafy-dark-gray">
              {badge}
            </span>
          </div>
        ))}
      </div>
    </div>
  </div>
);
