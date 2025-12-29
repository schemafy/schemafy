import { useContext } from 'react';
import { ViewportContext } from './viewport.context';

export const useViewportContext = () => {
  const context = useContext(ViewportContext);
  if (!context) {
    throw new Error('useViewportContext must be used within ViewportProvider');
  }
  return context;
};
