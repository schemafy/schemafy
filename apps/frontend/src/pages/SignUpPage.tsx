import { Button } from '@/components';
import { SignUpForm } from '@/features/auth';

export const SignUpPage = () => {
  return (
    <div className="py-5 flex flex-col justify-center items-center w-full">
      <section className="flex flex-col px-4 py-3 w-full items-center justify-center">
        <h2 className="font-heading-xl">Join Us</h2>
        <p className="font-body-md text-schemafy-dark-gray mb-2.5">
          Create your account to get started
        </p>
      </section>
      <SignUpForm />
      <div className="flex gap-2 pt-1 pb-3">
        <p className="font-body-sm text-schemafy-dark-gray">
          Already have an account?
        </p>
        <Button
          variant={'none'}
          size={'none'}
          className="text-schemafy-dark-gray"
        >
          Sign In
        </Button>
      </div>
    </div>
  );
};
