import { Button, InputField } from '@/components';
import { useFormState } from '../hooks';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signUp } from '@/features/auth/api';
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
    if (value.trim().length > 200)
      return 'Name must be 200 characters or less.';
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
    if (value.length < 8) return 'Password must be at least 8 characters.';
    return '';
  },
  confirmPassword: (value: string, form?: SignUpFormValues) => {
    if (!value.trim()) return 'Please confirm your password.';
    if (form && value !== form.password) return 'Password does not match.';
    return '';
  },
};

export const SignUpForm = () => {
  const { form, errors, handleChange, handleBlur, resetForm } = useFormState(
    initialForm,
    validationRules,
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const hasErrors = Object.keys(errors).length > 0;
    if (hasErrors) {
      return;
    }

    setIsSubmitting(true);

    try {
      await signUp({
        email: form.email,
        name: form.name,
        password: form.password,
      });

      resetForm();
      navigate('/');
    } catch {
      // publicClient interceptor
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      noValidate
      className="flex flex-col w-full max-w-[480px]"
      onSubmit={handleSubmit}
    >
      {formFields.map((field) => (
        <InputField
          key={field.name}
          label={field.label}
          type={field.type}
          name={field.name}
          placeholder={field.label}
          disabled={isSubmitting}
          value={form[field.name]}
          error={errors[field.name]}
          onChange={handleChange}
          onBlur={handleBlur}
        />
      ))}
      <Button type="submit" disabled={isSubmitting} className="my-4" round>
        {isSubmitting ? 'Creating...' : 'Create Account'}
      </Button>
    </form>
  );
};
