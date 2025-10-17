import { createContext } from 'react';
import { type ThemeProviderState, initialState } from './theme.types';

export const ThemeProviderContext =
  createContext<ThemeProviderState>(initialState);
