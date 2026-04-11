import { Button } from '@/components';
import { SignInForm } from '@/features/auth';
import { useSearch } from '@tanstack/react-router';

export const SignInPage = () => {
  const oauthError = useSearch({
    strict: false,
    select: (search) => {
      const value = (search as { oauthError?: unknown }).oauthError;
      return typeof value === 'string' ? value : null;
    },
  });

  return (
    <div className="py-5 flex flex-col justify-center items-center w-full">
      <h2 className="font-heading-xl mb-3 mt-5">Sign in to your account</h2>
      <SignInForm oauthError={oauthError} />
      <div className="flex gap-2 pt-1 pb-3 items-center justify-center">
        <p className="font-body-sm text-schemafy-dark-gray">
          Don't have an account?
        </p>
        <Button
          variant={'none'}
          size={'none'}
          className="text-schemafy-dark-gray"
          to="/signup"
        >
          Sign Up
        </Button>
      </div>
    </div>
  );
};
