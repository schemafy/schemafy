import React from 'react';
import { useMemoStore, MemoContext } from '../../memo/hooks/useMemoStore';

export const MemoProvider = ({ children }: { children: React.ReactNode }) => {
  const value = useMemoStore();

  return <MemoContext.Provider value={value}>{children}</MemoContext.Provider>;
};
