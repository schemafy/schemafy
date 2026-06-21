import { Button, InputField } from '@/components';
import { useFormState } from '../hooks';
import { useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import {
  sendSignUpEmailCode,
  signUp,
  verifySignUpEmail,
} from '@/features/auth/api';
import type { SignUpChallengeResponse } from '@/features/auth/api/types';
import { authStore } from '@/store/auth.store';
import { toast } from 'sonner';
import type {
  SignUpFormValues,
  SignUpVerificationFormValues,
  ValidationRules,
} from '../types';

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

const initialVerificationForm: SignUpVerificationFormValues = {
  code: '',
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

const verificationValidationRules: ValidationRules<SignUpVerificationFormValues> =
  {
    code: (value: string) => {
      if (!value.trim()) return 'Verification code is required.';
      if (!/^\d{6}$/.test(value)) return 'Enter the 6-digit code.';
      return '';
    },
  };

export const SignUpForm = () => {
  const { form, errors, handleChange, handleBlur, resetForm } = useFormState(
    initialForm,
    validationRules,
  );
  const {
    form: verificationForm,
    errors: verificationErrors,
    handleChange: handleVerificationChange,
    handleBlur: handleVerificationBlur,
    resetForm: resetVerificationForm,
  } = useFormState(initialVerificationForm, verificationValidationRules);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);
  const [sentEmail, setSentEmail] = useState<string | null>(null);
  const [lastEmailChallenge, setLastEmailChallenge] =
    useState<SignUpChallengeResponse | null>(null);
  const [signupVerificationToken, setSignupVerificationToken] = useState('');
  const [formError, setFormError] = useState('');
  const navigate = useNavigate();

  const emailError = validationRules.email?.(form.email, form) ?? '';
  const verificationCodeError =
    verificationValidationRules.code?.(
      verificationForm.code,
      verificationForm,
    ) ?? '';

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.name === 'email') {
      setSentEmail(null);
      setSignupVerificationToken('');
      resetVerificationForm();
    }
    setFormError('');
    handleChange(e);
  };

  const handleVerificationCodeChange = (
    e: React.ChangeEvent<HTMLInputElement>,
  ) => {
    setSignupVerificationToken('');
    setFormError('');
    handleVerificationChange(e);
  };

  const hasActiveEmailChallenge = (
    previous: SignUpChallengeResponse | null,
    current: SignUpChallengeResponse,
  ) => {
    if (!previous || previous.email !== current.email) {
      return false;
    }

    const expiresAt = Date.parse(previous.expiresAt);
    return Number.isFinite(expiresAt) && expiresAt > Date.now();
  };

  const handleSendCode = async () => {
    if (emailError) {
      setFormError(emailError);
      return;
    }

    setIsSendingCode(true);
    setFormError('');

    try {
      const challenge = await sendSignUpEmailCode({ email: form.email });
      const alreadySent = hasActiveEmailChallenge(
        lastEmailChallenge,
        challenge,
      );

      setSentEmail(challenge.email);
      setLastEmailChallenge(challenge);
      setSignupVerificationToken('');
      resetVerificationForm();

      if (alreadySent) {
        toast.info(
          <>
            A verification email was already sent.
            <br />
            Check your inbox or use the existing code.
          </>,
        );
      } else {
        toast.success('Verification email sent. Please check your inbox.');
      }
    } catch {
    } finally {
      setIsSendingCode(false);
    }
  };

  const handleVerifyCode = async () => {
    if (!sentEmail) {
      setFormError('Please send a verification code first.');
      return;
    }
    if (verificationCodeError) {
      setFormError(verificationCodeError);
      return;
    }

    setIsVerifyingCode(true);
    setFormError('');

    try {
      const result = await verifySignUpEmail({
        email: sentEmail,
        code: verificationForm.code,
      });
      setSignupVerificationToken(result.signupVerificationToken);
    } catch {
    } finally {
      setIsVerifyingCode(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const currentErrors = [
      validationRules.email?.(form.email, form),
      validationRules.name?.(form.name, form),
      validationRules.password?.(form.password, form),
      validationRules.confirmPassword?.(form.confirmPassword, form),
    ].filter(Boolean);
    const hasErrors = Object.keys(errors).length > 0;
    if (hasErrors || currentErrors.length > 0) {
      setFormError(currentErrors[0] ?? 'Please check your input.');
      return;
    }
    if (!signupVerificationToken) {
      setFormError('Please verify your email before creating an account.');
      return;
    }

    setIsSubmitting(true);

    try {
      const user = await signUp({
        email: form.email,
        name: form.name,
        password: form.password,
        signupVerificationToken,
      });

      authStore.setUser(user);
      resetForm();
      resetVerificationForm();
      setSentEmail(null);
      setLastEmailChallenge(null);
      setSignupVerificationToken('');
      navigate({ to: '/' });
    } catch {
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
      <div className="flex items-start gap-2">
        <InputField
          label="Email"
          type="email"
          name="email"
          placeholder="Email"
          required
          disabled={isSubmitting || isSendingCode || isVerifyingCode}
          value={form.email}
          error={errors.email}
          onChange={handleFormChange}
          onBlur={handleBlur}
        />
        <Button
          type="button"
          disabled={isSubmitting || isSendingCode || isVerifyingCode}
          className="mt-[52px] min-w-[88px]"
          onClick={handleSendCode}
        >
          {isSendingCode ? 'Sending...' : 'Send'}
        </Button>
      </div>
      <div className="flex items-start gap-2">
        <InputField
          label="Verification Code"
          type="text"
          name="code"
          placeholder="000000"
          required
          disabled={
            !sentEmail || isSubmitting || isSendingCode || isVerifyingCode
          }
          value={verificationForm.code}
          error={verificationErrors.code}
          onChange={handleVerificationCodeChange}
          onBlur={handleVerificationBlur}
        />
        <Button
          type="button"
          disabled={
            !sentEmail || isSubmitting || isSendingCode || isVerifyingCode
          }
          className="mt-[52px] min-w-[88px]"
          onClick={handleVerifyCode}
        >
          {isVerifyingCode ? 'Verifying...' : 'Verify'}
        </Button>
      </div>
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
          onChange={handleFormChange}
          onBlur={handleBlur}
        />
      ))}
      {signupVerificationToken && (
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
      <Button type="submit" disabled={isSubmitting} className="my-4" round>
        {isSubmitting ? 'Creating...' : 'Create Account'}
      </Button>
    </form>
  );
};
