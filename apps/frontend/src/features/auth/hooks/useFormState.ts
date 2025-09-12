import { useState, useCallback } from 'react';
import type { FormValues, ValidationRules } from '../types';

export const useFormState = <T extends FormValues>(
  initialForm: T,
  validationRules: ValidationRules<T> = {},
) => {
  const [form, setForm] = useState<T>(initialForm);
  const [errors, setErrors] = useState<Partial<Record<keyof T, string>>>({});

  const validateField = useCallback(
    (name: keyof T, value: T[keyof T]) => {
      const validator = validationRules[name];
      if (validator) {
        const error = validator(value, form);
        setErrors((prev) => ({ ...prev, [name]: error }));
      }
    },
    [validationRules, form],
  );

  const handleChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value as T[keyof T] }));
  }, []);

  const handleBlur = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      const { name, value } = e.target;
      validateField(name as keyof T, value as T[keyof T]);
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
