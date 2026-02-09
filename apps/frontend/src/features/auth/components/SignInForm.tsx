import { Button, InputField } from '@/components';
import type { SignInFormValues, ValidationRules } from '../types';
import { useFormState } from '../hooks';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signIn } from '@/lib/api';
import { authStore } from '@/store/auth.store';

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
    const hasErrors = Object.keys(errors).length > 0;
    if (hasErrors) {
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await signIn({
        email: form.email,
        password: form.password,
      });

      if (response.success) {
        if (response.result) {
          authStore.setUser(response.result);
        }
        resetForm();
        navigate('/');
      } else {
        setSubmitError(response.error?.message || '로그인에 실패했습니다.');
      }
    } catch (error) {
      setSubmitError(
        error instanceof Error ? error.message : '로그인에 실패했습니다.',
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
          required={field.required}
          value={form[field.name]}
          error={errors[field.name]}
          onChange={handleChange}
          onBlur={handleBlur}
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
        <Button type="submit" disabled={isSubmitting} round fullWidth>
          {isSubmitting ? 'Sign In...' : 'Sign In'}
        </Button>
        {submitError && (
          <p className="text-red-600 text-sm mt-2 mb-2">{submitError}</p>
        )}
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
