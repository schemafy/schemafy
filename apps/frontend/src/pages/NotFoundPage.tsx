import { Button } from '@/components';

export const NotFoundPage = () => {
  return (
    <div className="w-full flex-1 px-6 flex flex-col items-center justify-center text-center gap-6">
      <div className="flex flex-col items-center gap-3">
        <span className="font-overline-sm text-schemafy-dark-gray">
          Error 404
        </span>
        <h2 className="font-heading-xl text-schemafy-text">Page not found</h2>
        <p className="font-body-md text-schemafy-dark-gray max-w-md">
          The page you are looking for does not exist or may have been moved.
        </p>
      </div>

      <div className="flex gap-3">
        <Button to="/" round>
          Go Home
        </Button>
        <Button to="/workspace" variant="secondary" round>
          Open Workspace
        </Button>
      </div>
    </div>
  );
};
