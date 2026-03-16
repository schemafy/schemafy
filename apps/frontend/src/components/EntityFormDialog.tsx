import { useEffect, useState } from 'react';
import {
  Button,
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components';

interface EntityFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  submitLabel: string;
  initialName?: string;
  initialDescription?: string;
  isPending: boolean;
  onSubmit: (data: { name: string; description: string }) => void;
  namePlaceholder?: string;
  descriptionPlaceholder?: string;
}

export const EntityFormDialog = ({
  open,
  onOpenChange,
  title,
  submitLabel,
  initialName = '',
  initialDescription = '',
  isPending,
  onSubmit,
  namePlaceholder = 'Name',
  descriptionPlaceholder = 'Optional description',
}: EntityFormDialogProps) => {
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription);

  useEffect(() => {
    if (open) {
      setName(initialName);
      setDescription(initialDescription);
    }
  }, [open, initialName, initialDescription]);

  const handleSubmit = () => {
    if (!name.trim()) return;
    onSubmit({ name, description: description || '' });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4 py-2">
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={namePlaceholder}
              className="w-full px-4 py-3 border border-schemafy-light-gray rounded-[12px] font-body-sm placeholder-schemafy-dark-gray bg-schemafy-bg focus:outline-none"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="font-overline-xs text-schemafy-dark-gray">Description</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={descriptionPlaceholder}
              className="w-full px-4 py-3 border border-schemafy-light-gray rounded-[12px] font-body-sm placeholder-schemafy-dark-gray bg-schemafy-bg focus:outline-none"
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button size="sm" onClick={handleSubmit} disabled={!name.trim() || isPending}>
            {submitLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
