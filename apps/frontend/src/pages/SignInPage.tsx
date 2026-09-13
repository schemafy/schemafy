import { useEffect, useRef } from 'react';
import { CheckCircle2, Database, GitBranch, KeyRound } from 'lucide-react';
import { ButtonLink } from '@/components';
import { SignInForm } from '@/features/auth';
import { useNavigate, useSearch } from '@tanstack/react-router';
import { notifyAuthRequired } from '@/lib/api/error-handler';

export const SignInPage = () => {
  const navigate = useNavigate();
  const { oauthError, authRequired } = useSearch({ from: '/signin' });
  const hasHandledAuthRequired = useRef(false);

  useEffect(() => {
    if (!authRequired || hasHandledAuthRequired.current) return;

    hasHandledAuthRequired.current = true;
    notifyAuthRequired();
    void navigate({
      to: '/signin',
      replace: true,
      search: { oauthError },
    });
  }, [authRequired, navigate, oauthError]);

  return (
    <div className="relative flex min-h-[calc(100vh-4.25rem)] w-full items-center justify-center overflow-hidden px-4 py-8 sm:px-6 lg:px-8">
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(90deg,hsl(var(--schemafy-light-gray)/0.12)_1px,transparent_1px),linear-gradient(180deg,hsl(var(--schemafy-light-gray)/0.12)_1px,transparent_1px)] bg-[size:32px_32px]" />
      <div className="schemafy-page-card relative grid w-full max-w-5xl overflow-hidden lg:grid-cols-[1.05fr_0.95fr]">
        <section className="hidden min-h-[34rem] flex-col justify-between border-r border-schemafy-glass-border bg-schemafy-panel/70 p-8 lg:flex">
          <div className="flex flex-col gap-5">
            <span className="schemafy-badge w-fit px-3 py-1 font-caption-md">
              ERD workspace
            </span>
            <div className="flex flex-col gap-3">
              <h2 className="font-heading-xl text-schemafy-text">
                Design schemas with a calm, precise canvas.
              </h2>
              <p className="font-body-sm text-schemafy-dark-gray">
                Open your projects, collaborate with your team, and keep schema
                decisions in one polished workspace.
              </p>
            </div>
          </div>
          <AuthCanvasPreview />
          <div className="grid grid-cols-2 gap-3">
            {['ERD canvas', 'DDL export', 'Team roles', 'Memo threads'].map(
              (item) => (
                <div
                  key={item}
                  className="rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/60 px-3 py-2 font-caption-md text-schemafy-dark-gray"
                >
                  {item}
                </div>
              ),
            )}
          </div>
        </section>
        <section className="flex flex-col items-center justify-center px-5 py-8 sm:px-10 sm:py-10 lg:px-12">
          <div className="mb-6 flex w-full max-w-[480px] flex-col gap-2">
            <h2 className="font-heading-xl text-schemafy-text">
              Sign in to Schemafy
            </h2>
            <p className="font-body-sm text-schemafy-dark-gray">
              Continue to your database design workspace.
            </p>
          </div>
          <SignInForm oauthError={oauthError} />
          <div className="flex flex-wrap items-center justify-center gap-x-2 gap-y-1 pb-2 pt-4">
            <p className="font-body-sm text-schemafy-dark-gray">
              Don't have an account?
            </p>
            <ButtonLink
              variant={'none'}
              size={'none'}
              className="schemafy-menu-button px-2 py-1 text-schemafy-text"
              to="/signup"
            >
              Sign Up
            </ButtonLink>
          </div>
        </section>
      </div>
    </div>
  );
};

const AuthCanvasPreview = () => {
  const checks = [
    ['Primary keys visible', KeyRound],
    ['Relations visible', GitBranch],
    ['Memos attached', CheckCircle2],
  ] as const;

  return (
    <div className="schemafy-strong-panel rounded-2xl p-4">
      <div className="mb-4 flex items-center justify-between gap-4 border-b border-schemafy-glass-border pb-3">
        <div className="flex items-center gap-2">
          <Database className="h-4 w-4 text-schemafy-soft-blue" />
          <span className="font-heading-xs text-schemafy-text">
            Design Preview
          </span>
        </div>
        <span className="rounded-full bg-schemafy-green/12 px-2.5 py-1 font-caption-sm text-schemafy-green">
          Ready
        </span>
      </div>
      <div className="grid gap-3">
        {checks.map(([label, Icon]) => (
          <div
            key={label}
            className="flex items-center justify-between gap-3 rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/55 px-3 py-2.5"
          >
            <span className="font-caption-md text-schemafy-text">{label}</span>
            <Icon className="h-4 w-4 text-schemafy-soft-blue" />
          </div>
        ))}
      </div>
    </div>
  );
};
