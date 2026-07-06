import { Button, InputField } from '@/components';
import type { SignInFormValues, ValidationRules } from '../types';
import { useFormState } from '../hooks';
import { useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { signIn } from '@/features/auth/api';
import { authStore } from '@/store/auth.store';
import { gitHubLogin } from '@/features/auth/lib/oauth';
import { reportUnexpectedError } from '@/lib';

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

interface SignInFormProps {
  oauthError: string | null;
}

export const SignInForm = ({ oauthError }: SignInFormProps) => {
  const { form, errors, handleChange, handleBlur, resetForm } = useFormState(
    initialForm,
    validationRules,
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleGithubLogin = () => {
    gitHubLogin();
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const hasErrors = Object.keys(errors).length > 0;
    if (hasErrors) {
      return;
    }

    setIsSubmitting(true);

    try {
      const user = await signIn({
        email: form.email,
        password: form.password,
      });

      authStore.setUser(user);
      resetForm();
      navigate({ to: '/' });
    } catch (error) {
      reportUnexpectedError(error, {
        context: 'Unexpected sign-in form failure.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      noValidate
      className="flex w-full max-w-[480px] flex-col gap-2"
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
        className="schemafy-menu-button ml-auto px-3 py-1.5 text-schemafy-dark-gray"
      >
        Forgot Password?
      </Button>
      <div className="flex w-full flex-col gap-3 pt-3">
        <Button type="submit" disabled={isSubmitting} round fullWidth>
          {isSubmitting ? 'Sign In...' : 'Sign In'}
        </Button>
        {oauthError && (
          <p className="rounded-xl border border-schemafy-destructive/30 bg-schemafy-destructive/10 px-3 py-2 font-body-sm text-schemafy-destructive">
            {oauthError}
          </p>
        )}
        <div className="flex w-full flex-col gap-2">
          <Button
            type="button"
            variant={'secondary'}
            round
            fullWidth
            onClick={handleGithubLogin}
          >
            Continue with GitHub
          </Button>
        </div>
      </div>
    </form>
  );
};
