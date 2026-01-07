import { type ReactNode } from 'react';
import { ViewportContext, type ViewportContextValue } from './viewport.context';

interface ViewportProviderProps {
  children: ReactNode;
  value: ViewportContextValue;
}

export const ViewportProvider = ({
  children,
  value,
}: ViewportProviderProps) => {
  return (
    <ViewportContext.Provider value={value}>
      {children}
    </ViewportContext.Provider>
  );
};
