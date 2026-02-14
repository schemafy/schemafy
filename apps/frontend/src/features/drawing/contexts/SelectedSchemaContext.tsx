import { useState, useEffect, type ReactNode } from 'react';
import { SelectedSchemaContext } from './useSelectedSchema';

const getStorageKey = (projectId: string) => `selectedSchemaId_${projectId}`;

interface SelectedSchemaProviderProps {
  children: ReactNode;
  projectId: string;
}

export const SelectedSchemaProvider = ({
  children,
  projectId,
}: SelectedSchemaProviderProps) => {
  const storageKey = getStorageKey(projectId);

  const [selectedSchemaId, setSelectedSchemaIdState] = useState<string | null>(
    () => {
      try {
        const stored = localStorage.getItem(storageKey);
        return stored;
      } catch {
        return null;
      }
    },
  );

  const setSelectedSchemaId = (schemaId: string | null) => {
    setSelectedSchemaIdState(schemaId);
    try {
      if (schemaId) {
        localStorage.setItem(storageKey, schemaId);
      } else {
        localStorage.removeItem(storageKey);
      }
    } catch (error) {
      console.error('Failed to update localStorage:', error);
    }
  };

  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === storageKey) {
        setSelectedSchemaIdState(e.newValue);
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [storageKey]);

  return (
    <SelectedSchemaContext.Provider
      value={{ selectedSchemaId, setSelectedSchemaId }}
    >
      {children}
    </SelectedSchemaContext.Provider>
  );
};
