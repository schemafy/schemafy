import { useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { signUp } from '@/features/auth/api';
import { authStore } from '@/store/auth.store';
import { reportUnexpectedError } from '@/lib';
import type { UseSignUpSubmissionProps } from '../types';

export const useSignUpSubmission = ({
  form,
  errors,
  validationRules,
  emailVerificationRequired,
  signupVerificationToken,
  onSuccess,
  setFormError,
}: UseSignUpSubmissionProps) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const currentErrors = [
      validationRules.email?.(form.email, form),
      validationRules.name?.(form.name, form),
      validationRules.password?.(form.password, form),
      validationRules.confirmPassword?.(form.confirmPassword, form),
    ].filter(Boolean);
    if (Object.keys(errors).length > 0 || currentErrors.length > 0) {
      setFormError(currentErrors[0] ?? 'Please check your input.');
      return;
    }
    if (emailVerificationRequired && !signupVerificationToken) {
      setFormError('Please verify your email to continue.');
      return;
    }

    setIsSubmitting(true);

    try {
      const user = await signUp({
        email: form.email,
        name: form.name,
        password: form.password,
        ...(emailVerificationRequired && { signupVerificationToken }),
      });

      authStore.setUser(user);
      onSuccess();
      navigate({ to: '/' });
    } catch (error) {
      reportUnexpectedError(error, {
        context: 'Unexpected sign-up form failure.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return {
    isSubmitting,
    handleSubmit,
  };
};
