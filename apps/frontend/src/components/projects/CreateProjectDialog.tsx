import { useState } from 'react';
import {
  Button,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components';
import type { ProjectRequest } from '@/lib/api';

interface CreateProjectDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: ProjectRequest) => void;
}

export const CreateProjectDialog = ({
  open,
  onOpenChange,
  onSubmit,
}: CreateProjectDialogProps) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const handleSubmit = () => {
    onSubmit({ name, description });
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
          <DialogTitle>Create Project</DialogTitle>
        </DialogHeader>
        <div className="py-2 flex flex-col gap-8 justify-between items-center rounded-[10px]">
          <div className="w-full bg-schemafy-secondary rounded-xl">
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              type="text"
              placeholder="Project name"
              className="w-full pl-3 pr-4 py-3 bg-schemafy-secondary rounded-xl font-body-xs text-schemafy-text placeholder-schemafy-dark-gray focus:outline-none focus:ring-2 focus:ring-schemafy-button-bg"
            />
          </div>
          <div className="w-full bg-schemafy-secondary rounded-xl">
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              type="text"
              placeholder="Project description"
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
