import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  LoadingSpinner,
} from '@/components';
import type { SchemaExportRequest, SchemaExportResult } from '../api';

export type ExportStatus = 'idle' | 'loading' | 'success' | 'error';

interface ExportModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  request: SchemaExportRequest | null;
  result: SchemaExportResult | null;
  status: ExportStatus;
}

const getExportCode = (result: SchemaExportResult) =>
  result.format === 'ddl' ? result.data.ddl : result.data.mermaid;

export const ExportModal = ({
  open,
  onOpenChange,
  request,
  result,
  status,
}: ExportModalProps) => {
  const title = request?.format === 'mermaid' ? 'Export Mermaid' : 'Export DDL';

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[80vh] max-w-3xl flex-col">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>

        {status === 'loading' && (
          <div
            className="flex min-h-48 flex-col items-center justify-center gap-3"
            role="status"
            aria-live="polite"
          >
            <LoadingSpinner className="h-8 w-8" />
            <span className="font-body-sm text-schemafy-dark-gray">
              Generating export...
            </span>
          </div>
        )}

        {status === 'error' && (
          <div
            className="flex min-h-48 items-center justify-center font-body-sm text-schemafy-dark-gray"
            role="alert"
          >
            Failed to export the schema.
          </div>
        )}

        {status === 'success' && request && result && (
          <div className="flex min-h-0 flex-1 flex-col gap-3">
            <span className="font-overline-sm text-schemafy-text">
              {request.schemaName}
            </span>
            <div className="schemafy-scrollbar min-h-0 flex-1 overflow-auto rounded-xl border border-schemafy-glass-border bg-schemafy-bg p-4">
              <pre className="min-w-max whitespace-pre font-mono text-sm text-schemafy-text">
                {getExportCode(result)}
              </pre>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};
