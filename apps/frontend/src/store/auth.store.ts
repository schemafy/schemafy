import { makeAutoObservable } from 'mobx';
import type { AuthResponse } from '../lib/api/auth/types';

export class AuthStore {
  private static instance: AuthStore;

  accessToken: string | null = null;
  user: AuthResponse | null = null;
  isAuthLoading: boolean = true;

  private constructor() {
    makeAutoObservable(this);
  }

  static getInstance(): AuthStore {
    if (!AuthStore.instance) {
      AuthStore.instance = new AuthStore();
    }
    return AuthStore.instance;
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
