import { useEffect } from 'react';
import { ensureAuthInitialized } from '@/features/auth/lib/auth-bootstrap';

export const useAuthBootstrap = () => {
  useEffect(() => {
    void ensureAuthInitialized();
  }, []);
};
