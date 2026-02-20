import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  ListItem,
} from '@/components';
import { Search, Table } from 'lucide-react';
import { useReactFlow } from '@xyflow/react';
import { useSelectedSchema } from '../contexts';
import { useSchemaSnapshots } from '../hooks/useSchemaSnapshots';

interface SearchEntitiesDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export const SearchEntitiesDialog = ({
  open,
  onOpenChange,
}: SearchEntitiesDialogProps) => {
  const [searchQuery, setSearchQuery] = useState('');

  const { selectedSchemaId } = useSelectedSchema();
  const { data: snapshotsData } = useSchemaSnapshots(selectedSchemaId);
  const { fitView } = useReactFlow();

  const tables = Object.values(snapshotsData).map((snapshot) => ({
    id: snapshot.table.id,
    name: snapshot.table.name,
    columnCount: snapshot.columns.length,
  }));

  const query = searchQuery.trim().toLowerCase();
  const filteredTables = query
    ? tables.filter((table) => table.name.toLowerCase().includes(query))
    : tables;

  const handleTableClick = (tableId: string) => {
    onOpenChange(false);
    fitView({ nodes: [{ id: tableId }], duration: 300, padding: 5 });
  };

  const handleOpenChange = (nextOpen: boolean) => {
    onOpenChange(nextOpen);
    if (!nextOpen) setSearchQuery('');
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <div className="flex gap-2.5 items-center">
            <Table size={16} color="var(--color-schemafy-dark-gray)" />
            <DialogTitle>Entities</DialogTitle>
          </div>
        </DialogHeader>
        <div className="py-2 px-3 flex justify-between items-center bg-schemafy-secondary rounded-[10px]">
          <input
            type="text"
            placeholder="Search for entities..."
            className="w-full focus:border-none outline-none focus:outline-none placeholder:text-schemafy-dark-gray border-none text-schemafy-text font-body-xs"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          <Search size={16} color="var(--color-schemafy-dark-gray)" />
        </div>
        <ul className="flex flex-col w-full max-h-[12.5rem] gap-2.5 overflow-y-scroll overflow-x-hidden pr-2 [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-schemafy-light-gray [&::-webkit-scrollbar-track]:bg-transparent">
          {filteredTables.map((table) => (
            <div
              key={table.id}
              onClick={() => handleTableClick(table.id)}
              className="cursor-pointer"
            >
              <ListItem count={table.columnCount} name={table.name} />
            </div>
          ))}
          {filteredTables.length === 0 && (
            <li className="flex items-center justify-center py-4 text-schemafy-dark-gray font-body-xs">
              No entities found
            </li>
          )}
        </ul>
      </DialogContent>
    </Dialog>
  );
};
