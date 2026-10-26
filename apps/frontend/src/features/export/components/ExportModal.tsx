import { useEffect, useRef, useState } from 'react';
import { Check, Copy, Download, RotateCcw } from 'lucide-react';
import { toast } from 'sonner';
import {
  Button,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  LoadingSpinner,
} from '@/components';
import type { SchemaExportRequest, SchemaExportResult } from '../api';
import { downloadExportFile } from '../lib';

export type ExportStatus = 'idle' | 'loading' | 'success' | 'error';

interface ExportModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  request: SchemaExportRequest | null;
  result: SchemaExportResult | null;
  status: ExportStatus;
  errorMessage: string | null;
  onRetry: () => void;
}

const getExportCode = (result: SchemaExportResult) =>
  result.format === 'ddl' ? result.data.ddl : result.data.mermaid;

interface ExportResultProps {
  request: SchemaExportRequest;
  result: SchemaExportResult;
}

const ExportResult = ({ request, result }: ExportResultProps) => {
  const [copied, setCopied] = useState(false);
  const copiedTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const code = getExportCode(result);

  useEffect(
    () => () => {
      if (copiedTimerRef.current) clearTimeout(copiedTimerRef.current);
    },
    [],
  );

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      if (copiedTimerRef.current) clearTimeout(copiedTimerRef.current);
      copiedTimerRef.current = setTimeout(() => setCopied(false), 2_000);
    } catch {
      toast.error('Failed to copy to clipboard. Please try again.');
    }
  };

  const handleDownload = () => {
    try {
      downloadExportFile({
        code,
        format: request.format,
        schemaName: request.schemaName,
      });
    } catch {
      toast.error('Failed to download the file. Please try again.');
    }
  };

  return (
    <div className="flex min-h-0 flex-1 flex-col gap-3">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <span className="font-overline-sm text-schemafy-text">
          {request.schemaName}
        </span>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={handleCopy}>
            {copied ? (
              <Check className="h-4 w-4" />
            ) : (
              <Copy className="h-4 w-4" />
            )}
            {copied ? 'Copied' : 'Copy'}
          </Button>
          <Button variant="outline" size="sm" onClick={handleDownload}>
            <Download className="h-4 w-4" />
            Download
          </Button>
        </div>
      </div>
      <div className="schemafy-scrollbar min-h-0 flex-1 overflow-auto rounded-xl border border-schemafy-glass-border bg-schemafy-bg p-4">
        <pre className="min-w-max whitespace-pre font-mono text-sm text-schemafy-text">
          {code}
        </pre>
      </div>
    </div>
  );
};

export const ExportModal = ({
  open,
  onOpenChange,
  request,
  result,
  status,
  errorMessage,
  onRetry,
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
            className="flex min-h-48 flex-col items-center justify-center gap-4 text-center font-body-sm text-schemafy-dark-gray"
            role="alert"
          >
            <span>{errorMessage}</span>
            <Button variant="outline" size="sm" onClick={onRetry}>
              <RotateCcw className="h-4 w-4" />
              Retry
            </Button>
          </div>
        )}

        {status === 'success' && request && result && (
          <ExportResult request={request} result={result} />
        )}
      </DialogContent>
    </Dialog>
  );
};
