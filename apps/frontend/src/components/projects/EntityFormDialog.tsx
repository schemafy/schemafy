import { useState, useEffect } from 'react';
import {
  Button,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components';

type EntityType = 'workspace' | 'project';
type Mode = 'create' | 'edit';

export type EntityFormData = {
  id?: string;
  name: string;
  description: string;
};

interface EntityFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: EntityFormData) => void;
  entityType: EntityType;
  mode: Mode;
  initialData?: EntityFormData;
}

const ENTITY_LABELS: Record<EntityType, { singular: string }> = {
  workspace: { singular: 'Workspace' },
  project: { singular: 'Project' },
};

export const EntityFormDialog = ({
  open,
  onOpenChange,
  onSubmit,
  entityType,
  mode,
  initialData,
}: EntityFormDialogProps) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const label = ENTITY_LABELS[entityType];
  const title =
    mode === 'create' ? `Create ${label.singular}` : `Edit ${label.singular}`;

  useEffect(() => {
    if (open) {
      if (mode === 'edit' && initialData) {
        setName(initialData.name);
        setDescription(initialData.description);
      } else {
        setName('');
        setDescription('');
      }
    }
  }, [open, mode, initialData]);

  const handleSubmit = () => {
    onSubmit({ id: initialData?.id, name, description });
    setName('');
    setDescription('');
  };

  const handleClose = () => {
    onOpenChange(false);
    setName('');
    setDescription('');
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <div className="py-2 flex flex-col gap-8 justify-between items-center rounded-[10px]">
          <div className="w-full bg-schemafy-secondary rounded-xl">
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              type="text"
              placeholder={`${label.singular} name`}
              className="w-full pl-3 pr-4 py-3 bg-schemafy-secondary rounded-xl font-body-xs text-schemafy-text placeholder-schemafy-dark-gray focus:outline-none focus:ring-2 focus:ring-schemafy-button-bg"
            />
          </div>
          <div className="w-full bg-schemafy-secondary rounded-xl">
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              type="text"
              placeholder={`${label.singular} description`}
              className="w-full pl-3 pr-4 py-3 bg-schemafy-secondary rounded-xl font-body-xs text-schemafy-text placeholder-schemafy-dark-gray focus:outline-none focus:ring-2 focus:ring-schemafy-button-bg"
            />
          </div>
        </div>
        <div className="flex gap-2">
          <Button onClick={handleSubmit}>Save</Button>
          <Button variant="outline" onClick={handleClose}>
            Cancel
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
