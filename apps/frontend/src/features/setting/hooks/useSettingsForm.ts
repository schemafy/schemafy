import { useContext, useState } from 'react';

import { ThemeProviderContext, type Theme } from '@/lib/config';
import { authStore } from '@/store/auth.store';

export const useSettingsForm = () => {
  const { theme, setTheme } = useContext(ThemeProviderContext);
  const user = authStore.user;

  const [name, setName] = useState(user?.name ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

  const handleThemeChange = (value: string) => {
    setTheme(value as Theme);
  };

  const openDeleteDialog = () => {
    setIsDeleteDialogOpen(true);
  };

  const confirmDeleteAccount = () => {};

  return {
    theme,
    handleThemeChange,
    name,
    setName,
    email,
    setEmail,
    password,
    setPassword,
    confirmPassword,
    setConfirmPassword,
    isDeleteDialogOpen,
    setIsDeleteDialogOpen,
    openDeleteDialog,
    confirmDeleteAccount,
  };
};
