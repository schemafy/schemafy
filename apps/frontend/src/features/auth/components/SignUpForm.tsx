import { Button, InputField } from '@/components';
import { useFormState } from '../hooks';
import { useFormStatus } from 'react-dom';
import type { ValidationRules, SignUpFormValues } from '../types';

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

const validationRules: ValidationRules<SignUpFormValues> = {
  name: (value: string) => {
    if (!value.trim()) return 'Name is required.';
    return '';
  },
  email: (value: string) => {
    if (!value.trim()) return 'Email is required.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
      return 'Please enter a valid email address.';
    }
    return '';
  },
  password: (value: string) => {
    if (!value.trim()) return 'Password is required.';
    return '';
  },
  confirmPassword: (value: string, form?: SignUpFormValues) => {
    if (!value.trim()) return 'Please confirm your password.';
    if (form && value !== form.password) return 'Password does not match.';
    return '';
  },
};

export const SignUpForm = () => {
  const { form, errors, handleChange, handleBlur } = useFormState(
    initialForm,
    validationRules,
  );
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
