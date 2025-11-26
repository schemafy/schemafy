import { create } from 'zustand';
import type { AuthResponse } from '../lib/api/auth/types';

interface AuthState {
  accessToken: string | null;
  user: AuthResponse | null;
  isAuthLoading: boolean;
  setAccessToken: (token: string | null) => void;
  setUser: (user: AuthResponse | null) => void;
  setAuthLoading: (value: boolean) => void;
  clearAccessToken: () => void;
  clearUser: () => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  isAuthLoading: false,
  setAccessToken: (token) => set({ accessToken: token }),
  setUser: (user) => set({ user }),
  setAuthLoading: (value) => set({ isAuthLoading: value }),
  clearAccessToken: () => set({ accessToken: null }),
  clearUser: () => set({ user: null }),
  clearAuth: () => set({ accessToken: null, user: null }),
}));
