import { ArrowRight, CheckCircle2, Database, GitBranch } from 'lucide-react';
import { Button } from '@/components';

export const ReadyToUse = () => {
  return (
    <section className="mx-auto w-full max-w-7xl py-10">
      <div className="schemafy-page-card overflow-hidden px-5 py-8 sm:px-8 lg:px-10">
        <div className="flex flex-col gap-8 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex max-w-2xl flex-col items-start gap-4">
            <span className="schemafy-badge px-3 py-1 font-caption-md">
              Ready workspace
            </span>
            <h3 className="font-heading-xl text-schemafy-text">
              Ready to design your next database?
            </h3>
            <p className="font-body-md text-schemafy-dark-gray">
              Start from a clear canvas with schema structure, relationship
              context, and team feedback already shaped for database designers.
            </p>
            <Button className="px-6" round>
              Get Started
              <ArrowRight className="h-4 w-4" />
            </Button>
          </div>
          <div className="schemafy-strong-panel w-full max-w-md rounded-2xl p-4">
            <div className="flex items-center justify-between gap-4 border-b border-schemafy-glass-border pb-3">
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
            <div className="grid gap-3 pt-4">
              {[
                ['12 tables', 'Entity map is organized'],
                ['28 relations', 'Foreign keys are visible'],
                ['4 memos', 'Comments are attached'],
              ].map(([metric, detail], index) => (
                <div
                  key={metric}
                  className="flex items-center justify-between gap-4 rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/55 px-3 py-2.5"
                >
                  <div className="flex min-w-0 items-center gap-2">
                    {index === 1 ? (
                      <GitBranch className="h-4 w-4 shrink-0 text-schemafy-violet" />
                    ) : (
                      <CheckCircle2 className="h-4 w-4 shrink-0 text-schemafy-green" />
                    )}
                    <span className="truncate font-caption-md text-schemafy-text">
                      {metric}
                    </span>
                  </div>
                  <span className="truncate text-right font-caption-sm text-schemafy-dark-gray">
                    {detail}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
