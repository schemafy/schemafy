import { create } from 'zustand';
import type { AuthResponse } from '../lib/api/auth/types';

interface AuthState {
  accessToken: string | null;
  user: AuthResponse | null;
  setAccessToken: (token: string | null) => void;
  setUser: (user: AuthResponse | null) => void;
  clearAccessToken: () => void;
  clearUser: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  setAccessToken: (token) => set({ accessToken: token }),
  setUser: (user) => set({ user }),
  clearAccessToken: () => set({ accessToken: null }),
  clearUser: () => set({ user: null }),
}));
