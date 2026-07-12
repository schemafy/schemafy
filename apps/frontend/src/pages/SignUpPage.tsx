import {
  CheckCircle2,
  LockKeyhole,
  MessageSquare,
  UsersRound,
} from 'lucide-react';
import { ButtonLink } from '@/components';
import { SignUpForm } from '@/features/auth';

export const SignUpPage = () => {
  return (
    <div className="relative flex min-h-[calc(100vh-4.25rem)] w-full items-center justify-center overflow-hidden px-4 py-8 sm:px-6 lg:px-8">
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(90deg,hsl(var(--schemafy-light-gray)/0.12)_1px,transparent_1px),linear-gradient(180deg,hsl(var(--schemafy-light-gray)/0.12)_1px,transparent_1px)] bg-[size:32px_32px]" />
      <div className="schemafy-page-card relative grid w-full max-w-5xl overflow-hidden lg:grid-cols-[0.95fr_1.05fr]">
        <section className="flex flex-col items-center justify-center px-5 py-8 sm:px-10 sm:py-10 lg:px-12">
          <div className="mb-6 flex w-full max-w-[480px] flex-col gap-2">
            <span className="schemafy-badge w-fit px-3 py-1 font-caption-md">
              Create workspace access
            </span>
            <h2 className="font-heading-xl text-schemafy-text">
              Join Schemafy
            </h2>
            <p className="font-body-sm text-schemafy-dark-gray">
              Create your account and start designing ERDs with a focused team
              workspace.
            </p>
          </div>
          <SignUpForm />
          <div className="flex flex-wrap items-center justify-center gap-x-2 gap-y-1 pb-2 pt-4">
            <p className="font-body-sm text-schemafy-dark-gray">
              Already have an account?
            </p>
            <ButtonLink
              variant={'none'}
              size={'none'}
              className="schemafy-menu-button px-2 py-1 text-schemafy-text"
              to="/signin"
            >
              Sign In
            </ButtonLink>
          </div>
        </section>
        <section className="hidden min-h-[36rem] flex-col justify-between border-l border-schemafy-glass-border bg-schemafy-panel/70 p-8 lg:flex">
          <div className="flex flex-col gap-4">
            <span className="schemafy-badge w-fit px-3 py-1 font-caption-md">
              Team-ready by default
            </span>
            <h3 className="font-heading-lg text-schemafy-text">
              Everything stays structured from the first table.
            </h3>
            <p className="font-body-sm text-schemafy-dark-gray">
              Invite collaborators, annotate schema decisions, and keep project
              context close to the ERD canvas.
            </p>
          </div>
          <SignUpAccessPreview />
          <div className="flex flex-col gap-3">
            {['Workspaces', 'Role-based access', 'Project sharing'].map(
              (item) => (
                <div
                  key={item}
                  className="rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/60 px-4 py-3 font-body-sm text-schemafy-text"
                >
                  {item}
                </div>
              ),
            )}
          </div>
        </section>
      </div>
    </div>
  );
};

const SignUpAccessPreview = () => {
  const rows = [
    ['Workspace access', UsersRound],
    ['Email/password account', LockKeyhole],
    ['Memo comments', MessageSquare],
  ] as const;

  return (
    <div className="schemafy-strong-panel rounded-2xl p-4">
      <div className="mb-4 flex items-center justify-between gap-4 border-b border-schemafy-glass-border pb-3">
        <span className="font-heading-xs text-schemafy-text">
          Onboarding checklist
        </span>
        <span className="rounded-full bg-schemafy-soft-blue/12 px-2.5 py-1 font-caption-sm text-schemafy-soft-blue">
          Ready
        </span>
      </div>
      <div className="grid gap-3">
        {rows.map(([label, Icon]) => (
          <div
            key={label}
            className="flex items-center justify-between gap-3 rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/55 px-3 py-2.5"
          >
            <div className="flex min-w-0 items-center gap-2">
              <Icon className="h-4 w-4 shrink-0 text-schemafy-violet" />
              <span className="truncate font-caption-md text-schemafy-text">
                {label}
              </span>
            </div>
            <CheckCircle2 className="h-4 w-4 shrink-0 text-schemafy-green" />
          </div>
        ))}
      </div>
    </div>
  );
};
