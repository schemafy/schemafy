import { useMutation } from '@tanstack/react-query';
import { exportSchema } from '../api';

export const useSchemaExport = () =>
  useMutation({
    mutationFn: exportSchema,
  });
