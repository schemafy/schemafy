import { Button, InputField } from '@/components';
import { useState } from 'react';
import {
  useFormState,
  useSignUpEmailVerification,
  useSignUpSubmission,
} from '../hooks';
import type { SignUpFormValues, ValidationRules } from '../types';

const formFields = [
  {
    label: 'Name',
    type: 'text' as const,
    name: 'name' as const,
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

const emailVerificationRequired = import.meta.env.SMTP_ENABLED !== 'false';

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
  const {
    form,
    errors,
    handleChange,
    handleBlur,
    runAllValidations,
    resetForm,
  } = useFormState(initialForm, validationRules);

  const [formError, setFormError] = useState('');
  const clearFormError = () => setFormError('');
  const emailError = validationRules.email?.(form.email, form) ?? '';

  const emailVerification = useSignUpEmailVerification({
    emailError,
    setFormError,
    clearFormError,
  });

  const signUpSubmission = useSignUpSubmission({
    form,
    runAllValidations,
    emailVerificationRequired,
    signupVerificationToken: emailVerification.signupVerificationToken,
    setFormError,
    onSuccess: () => {
      resetForm();
      emailVerification.resetVerification();
    },
  });

  const isVerificationDisabled =
    signUpSubmission.isSubmitting || emailVerification.isVerificationPending;
  const handleFormChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    clearFormError();
    if (event.target.name === 'email') {
      emailVerification.resetVerification();
    }
    handleChange(event);
  };

  return (
    <form
      noValidate
      className="flex w-full max-w-[480px] flex-col gap-2"
      onSubmit={signUpSubmission.handleSubmit}
    >
      <div className="flex items-start gap-2">
        <InputField
          label="Email"
          type="email"
          name="email"
          placeholder="Email"
          required
          disabled={isVerificationDisabled}
          value={form.email}
          error={errors.email}
          onChange={handleFormChange}
          onBlur={handleBlur}
        />
        {emailVerificationRequired && (
          <div className="flex shrink-0 flex-col gap-1.5 py-2.5 pr-4">
            <span aria-hidden="true" className="invisible font-overline-xs">
              Action
            </span>
            <Button
              type="button"
              disabled={isVerificationDisabled}
              className="h-[51px] min-w-[88px]"
              onClick={() => emailVerification.handleSendCode(form.email)}
            >
              {emailVerification.isSendingCode ? 'Sending...' : 'Send'}
            </Button>
          </div>
        )}
      </div>
      {emailVerificationRequired && (
        <div className="flex items-start gap-2">
          <InputField
            label="Verification Code"
            type="text"
            name="code"
            placeholder="000000"
            required
            disabled={!emailVerification.sentEmail || isVerificationDisabled}
            value={emailVerification.verificationForm.code}
            error={emailVerification.verificationErrors.code}
            onChange={emailVerification.handleVerificationCodeChange}
            onBlur={emailVerification.handleVerificationBlur}
          />
          <div className="flex shrink-0 flex-col gap-1.5 py-2.5 pr-4">
            <span aria-hidden="true" className="invisible font-overline-xs">
              Action
            </span>
            <Button
              type="button"
              disabled={!emailVerification.sentEmail || isVerificationDisabled}
              className="h-[51px] min-w-[88px]"
              onClick={emailVerification.handleVerifyCode}
            >
              {emailVerification.isVerifyingCode ? 'Verifying...' : 'Verify'}
            </Button>
          </div>
        </div>
      )}
      {formFields.map((field) => (
        <InputField
          key={field.name}
          label={field.label}
          type={field.type}
          name={field.name}
          placeholder={field.label}
          required={field.required}
          disabled={signUpSubmission.isSubmitting}
          value={form[field.name]}
          error={errors[field.name]}
          onChange={handleFormChange}
          onBlur={handleBlur}
        />
      ))}
      {emailVerificationRequired &&
        emailVerification.signupVerificationToken && (
          <p className="px-4 font-caption-md text-schemafy-primary">
            Email verified.
          </p>
        )}
      {formError && (
        <p
          className="px-4 text-schemafy-destructive font-caption-md"
          role="alert"
        >
          {formError}
        </p>
      )}
      <Button
        type="submit"
        disabled={signUpSubmission.isSubmitting}
        className="my-4"
        round
      >
        {signUpSubmission.isSubmitting ? 'Creating...' : 'Create Account'}
      </Button>
    </form>
  );
};
