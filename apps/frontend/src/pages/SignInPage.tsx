import { Button } from '@/components';
import { SignInForm } from '@/features/auth';
import { useLocation } from 'react-router-dom';

export const SignInPage = () => {
  const location = useLocation();
  const oauthError = location.state?.oauthError as string | null;

  return (
    <div className="py-5 flex flex-col justify-center items-center w-full">
      <h2 className="font-heading-xl mb-3 mt-5">Sign in to your account</h2>
      <SignInForm oauthError={oauthError}/>
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
