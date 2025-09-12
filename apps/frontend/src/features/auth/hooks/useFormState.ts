import type { SignUpFormValues } from '@/store/types';
import { useState, useCallback } from 'react';

type ValidationRules = {
  [K in keyof SignUpFormValues]?: (
    value: string,
    form?: SignUpFormValues,
  ) => string;
};

const validationRules: ValidationRules = {
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

export const useFormState = (initialForm: SignUpFormValues) => {
  const [form, setForm] = useState<SignUpFormValues>(initialForm);
  const [errors, setErrors] = useState<Partial<SignUpFormValues>>({});

  const validateField = useCallback(
    (name: keyof SignUpFormValues, value: string) => {
      const validator = validationRules[name];
      if (validator) {
        const error = validator(value, form);
        setErrors((prev) => ({ ...prev, [name]: error || undefined }));
      }
    },
    [form],
  );

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const { name, value } = e.target;
      setForm((prev) => ({ ...prev, [name]: value }));

      if (errors[name as keyof SignUpFormValues]) {
        setErrors((prev) => ({ ...prev, [name]: undefined }));
      }
    },
    [errors],
  );

  const handleBlur = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      const { name, value } = e.target;
      validateField(name as keyof SignUpFormValues, value);
    },
    [validateField],
  );

  const resetForm = useCallback(() => {
    setForm(initialForm);
    setErrors({});
  }, [initialForm]);

  return {
    form,
    errors,
    handleChange,
    handleBlur,
    resetForm,
  };
};
