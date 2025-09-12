import { Button, InputField } from '@/components';
import { useAuthAction, useFormState } from '../hooks';
import { useFormStatus } from 'react-dom';
import type { SignUpFormValues } from '@/store/types';

const formFields = [
  {
    label: 'Name',
    type: 'text' as const,
    name: 'name' as const,
    required: true,
  },
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
  {
    label: 'Confirm Password',
    type: 'password' as const,
    name: 'confirmPassword' as const,
    required: true,
  },
];

const initialForm: SignUpFormValues = {
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
};

export const SignUpForm = () => {
  const { form, errors, handleChange, handleBlur } = useFormState(initialForm);
  const { formAction } = useAuthAction();
  const { pending } = useFormStatus();

  return (
    <form noValidate action={formAction} className="flex flex-col w-full">
      {formFields.map((field) => (
        <InputField
          key={field.name}
          label={field.label}
          type={field.type}
          name={field.name}
          placeholder={field.label}
          disabled={pending}
          value={form[field.name]}
          error={errors[field.name]}
          onChange={handleChange}
          onBlur={handleBlur}
        />
      ))}
      <Button disabled={pending} className="my-4" round>
        {pending ? 'Creating...' : 'Create Account'}
      </Button>
    </form>
  );
};
