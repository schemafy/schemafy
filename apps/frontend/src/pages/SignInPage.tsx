import { useEffect, useRef } from 'react';
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
    <div className="py-5 flex flex-col justify-center items-center w-full">
      <h2 className="font-heading-xl mb-3 mt-5">Sign in to your account</h2>
      <SignInForm oauthError={oauthError} />
      <div className="flex gap-2 pt-1 pb-3 items-center justify-center">
        <p className="font-body-sm text-schemafy-dark-gray">
          Don't have an account?
        </p>
        <ButtonLink
          variant={'none'}
          size={'none'}
          className="text-schemafy-dark-gray"
          to="/signup"
        >
          Sign Up
        </ButtonLink>
      </div>
    </div>
  );
};
