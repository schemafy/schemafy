import { Button, InputField } from '@/components';
import { useFormState } from '../hooks';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signUp } from '@/lib/api';
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
  const [submitError, setSubmitError] = useState<string>('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubmitError('');

    const hasErrors = Object.values(errors).some(
      (error) => error !== undefined && error !== '',
    );
    if (hasErrors) {
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await signUp({
        email: form.email,
        name: form.name,
        password: form.password,
      });

      if (response.success) {
        resetForm();
        navigate('/');
      } else {
        setSubmitError(response.error?.message || '회원가입에 실패했습니다.');
      }
    } catch (error) {
      setSubmitError(
        error instanceof Error ? error.message : '회원가입에 실패했습니다.',
      );
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
      {submitError && (
        <p className="text-red-600 text-sm mt-2 mb-2">{submitError}</p>
      )}
      <Button type="submit" disabled={isSubmitting} className="my-4" round>
        {isSubmitting ? 'Creating...' : 'Create Account'}
      </Button>
    </form>
  );
};
