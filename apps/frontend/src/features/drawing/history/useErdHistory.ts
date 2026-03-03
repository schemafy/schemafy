import { useContext } from 'react';
import { ErdHistoryContext } from './ErdHistoryContext';

export const useErdHistory = () => {
  const context = useContext(ErdHistoryContext);
  if (!context) {
    throw new Error('useErdHistory must be used within ErdHistoryProvider');
  }
  return context;
};
