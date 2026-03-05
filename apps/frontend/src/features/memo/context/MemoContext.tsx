import React from 'react';
import { useMemoStore, MemoContext } from '../../memo/hooks/useMemoStore';
import { useSelectedSchema } from '@/features/drawing/contexts';

export const MemoProvider = ({ children }: { children: React.ReactNode }) => {
  const { selectedSchemaId } = useSelectedSchema();
  const value = useMemoStore(selectedSchemaId);

  return <MemoContext.Provider value={value}>{children}</MemoContext.Provider>;
};
