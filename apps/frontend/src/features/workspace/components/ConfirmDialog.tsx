import { Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, } from '@/components';

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel: string;
  onConfirm: () => void;
}

export const ConfirmDialog = ({
                                open,
                                onOpenChange,
                                title,
                                description,
                                confirmLabel,
                                onConfirm,
                              }: ConfirmDialogProps) => {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <p className="font-body-sm text-schemafy-dark-gray py-2">{description}</p>
        <DialogFooter>
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            variant="none"
            size="sm"
            className="bg-schemafy-destructive text-white"
            onClick={() => {
              onConfirm();
              onOpenChange(false);
            }}
          >
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};