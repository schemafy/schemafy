import { useEffect } from 'react';
import { ensureAuthInitialized } from '../lib/auth-bootstrap';

export const useAuthBootstrap = () => {
  useEffect(() => {
    void ensureAuthInitialized();
  }, []);
};
