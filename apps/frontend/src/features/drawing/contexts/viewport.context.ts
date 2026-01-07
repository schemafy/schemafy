import { createContext } from 'react';

export interface ViewportContextValue {
  selectedSchemaId: string | null;
  updateSelectedSchema: (schemaId: string) => void;
}

export const ViewportContext = createContext<ViewportContextValue | null>(null);
