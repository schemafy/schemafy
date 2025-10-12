import { Button, InputField } from '@/components';
import { useFormStatus } from 'react-dom';
import type { SignInFormValues, ValidationRules } from '../types';
import { useFormState } from '../hooks';

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

const initialForm: SignInFormValues = {
  email: '',
  password: '',
};

const validationRules: ValidationRules<SignInFormValues> = {
  email: (value: string) => {
    if (!value.trim()) return 'Email is required.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
      return 'Please enter a valid email address.';
    }
    return '';
  },
  password: (value: string) => {
    if (!value.trim()) return 'Password is required.';
    if (value.length < 8) return 'Password must be at least 8 characters.';
    return '';
  },
};

export const SignInForm = () => {
  const { form, errors, handleChange, handleBlur } = useFormState(initialForm, validationRules);
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
          value={form[field.name]}
          error={errors[field.name]}
          onChange={handleChange}
          onBlur={handleBlur}
        />
      ))}
      <Button variant={'none'} size={'none'} className="text-schemafy-dark-gray pt-1 pb-3">
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
