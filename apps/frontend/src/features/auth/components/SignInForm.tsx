import { Button, InputField } from '@/components';
import { useFormStatus } from 'react-dom';

const formFields = [
  {
    label: 'Email',
    type: 'email' as const,
    name: 'email' as const,
    required: true,
  },
  {
    label: 'Password',
    type: 'password' as const,
    name: 'password' as const,
    required: true,
  },
];

export const SignInForm = () => {
  const { pending } = useFormStatus();

  return (
    <form noValidate className="flex flex-col w-full max-w-[480px]">
      {formFields.map((field) => (
        <InputField
          key={field.name}
          label={field.label}
          type={field.type}
          name={field.name}
          placeholder={field.label}
          required={field.required}
          value={''}
          onChange={() => {}}
          error={''}
        />
      ))}
      <Button
        variant={'none'}
        size={'none'}
        className="text-schemafy-dark-gray pt-1 pb-3"
      >
        Forgot Password?
      </Button>
      <div className="flex flex-col w-full gap-6 py-3">
        <Button disabled={pending} round fullWidth>
          {pending ? 'Sign In...' : 'Sign In'}
        </Button>
        <div className="flex flex-col w-full gap-2">
          <Button variant={'secondary'} round fullWidth>
            Continue with GitHub
          </Button>
          <Button variant={'secondary'} round fullWidth>
            Continue with Google
          </Button>
        </div>
      </div>
    </form>
  );
};
