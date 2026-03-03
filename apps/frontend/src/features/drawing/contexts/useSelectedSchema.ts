import { createContext, useContext } from 'react';

interface SelectedSchemaContextType {
  projectId: string;
  selectedSchemaId: string;
  setSelectedSchemaId: (schemaId: string) => void;
}

export const SelectedSchemaContext = createContext<
  SelectedSchemaContextType | undefined
>(undefined);

export const useSelectedSchema = () => {
  const context = useContext(SelectedSchemaContext);
  if (context === undefined) {
    throw new Error(
      'useSelectedSchema must be used within a SelectedSchemaProvider',
    );
  }
  return context;
};
