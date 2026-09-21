import { useCallback, useState } from 'react';
import { sendSignUpEmailCode, verifySignUpEmail } from '@/features/auth/api';
import type { SignUpEmailVerificationResponse } from '@/features/auth/api/types';
import { reportUnexpectedError } from '@/lib';
import { toast } from 'sonner';
import { useFormState } from './useFormState';
import type {
  SignUpVerificationFormValues,
  UseSignUpEmailVerificationProps,
  ValidationRules,
} from '../types';

const initialVerificationForm: SignUpVerificationFormValues = {
  code: '',
};

const verificationValidationRules: ValidationRules<SignUpVerificationFormValues> =
  {
    code: (value: string) => {
      if (!value.trim()) return 'Verification code is required.';
      if (!/^\d{6}$/.test(value)) return 'Enter the 6-digit code.';
      return '';
    },
  };

export const useSignUpEmailVerification = ({
  emailError,
  setFormError,
  clearFormError,
}: UseSignUpEmailVerificationProps) => {
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);
  const [lastEmailVerification, setLastEmailVerification] =
    useState<SignUpEmailVerificationResponse | null>(null);
  const [signupVerificationToken, setSignupVerificationToken] = useState('');
  const [sentEmail, setSentEmail] = useState<string | null>(null);
  const {
    form: verificationForm,
    errors: verificationErrors,
    handleChange,
    handleBlur,
    resetForm,
  } = useFormState(initialVerificationForm, verificationValidationRules);

  const resetVerification = useCallback(() => {
    setLastEmailVerification(null);
    setSignupVerificationToken('');
    setSentEmail(null);
    clearFormError();
    resetForm();
  }, [clearFormError, resetForm]);

  const handleSendCode = async (email: string) => {
    if (emailError) {
      setFormError(emailError);
      return;
    }

    setIsSendingCode(true);
    clearFormError();

    try {
      const verification = await sendSignUpEmailCode({ email });
      const alreadySent = hasActiveEmailVerification(
        lastEmailVerification,
        verification,
      );

      setLastEmailVerification(verification);
      setSignupVerificationToken('');
      setSentEmail(verification.email);
      resetForm();

      if (alreadySent) {
        toast.info(
          'A verification email was already sent. Check your inbox or use the existing code.',
        );
      } else {
        toast.success('Verification email sent. Please check your inbox.');
      }
    } catch (error) {
      reportUnexpectedError(error, {
        context: 'Unexpected sign-up email code send failure.',
      });
    } finally {
      setIsSendingCode(false);
    }
  };

  const handleVerifyCode = async () => {
    if (!sentEmail) {
      setFormError('Please send a verification code first.');
      return;
    }

    const verificationCodeError =
      verificationValidationRules.code?.(
        verificationForm.code,
        verificationForm,
      ) ?? '';
    if (verificationCodeError) {
      setFormError(verificationCodeError);
      return;
    }

    setIsVerifyingCode(true);
    clearFormError();

    try {
      const result = await verifySignUpEmail({
        email: sentEmail,
        code: verificationForm.code,
      });
      setSignupVerificationToken(result.signupVerificationToken);
    } catch (error) {
      reportUnexpectedError(error, {
        context: 'Unexpected sign-up email verification failure.',
      });
    } finally {
      setIsVerifyingCode(false);
    }
  };

  const handleVerificationCodeChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    setSignupVerificationToken('');
    clearFormError();
    handleChange(event);
  };

  return {
    isSendingCode,
    isVerifyingCode,
    isVerificationPending: isSendingCode || isVerifyingCode,
    signupVerificationToken,
    sentEmail,
    verificationForm,
    verificationErrors,
    handleSendCode,
    handleVerifyCode,
    handleVerificationCodeChange,
    handleVerificationBlur: handleBlur,
    resetVerification,
  };
};

const hasActiveEmailVerification = (
  previous: SignUpEmailVerificationResponse | null,
  current: SignUpEmailVerificationResponse,
) => {
  if (previous?.email !== current.email) {
    return false;
  }

  const expiresAt = Date.parse(previous.expiresAt);
  return Number.isFinite(expiresAt) && expiresAt > Date.now();
};
