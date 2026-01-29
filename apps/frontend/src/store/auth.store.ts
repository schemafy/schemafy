import { makeAutoObservable } from 'mobx';
import { createContext, useContext } from 'react';
import type { AuthResponse } from '../lib/api/auth/types';

export class AuthStore {
  accessToken: string | null = null;
  user: AuthResponse | null = null;
  isAuthLoading: boolean = false;
  isInitialized: boolean = false;

  constructor() {
    makeAutoObservable(this);
  }

  setAccessToken(token: string | null) {
    this.accessToken = token;
  }

  setUser(user: AuthResponse | null) {
    this.user = user;
  }

  setAuthLoading(value: boolean) {
    this.isAuthLoading = value;
  }

  setInitialized(value: boolean) {
    this.isInitialized = value;
  }

  clearAccessToken() {
    this.accessToken = null;
  }

  clearUser() {
    this.user = null;
  }

  clearAuth() {
    this.accessToken = null;
    this.user = null;
  }
}

export const authStore = new AuthStore();

const AuthContext = createContext<AuthStore>(authStore);

export const AuthProvider = AuthContext.Provider;

export const useAuthStore = () => useContext(AuthContext);
