import { useState } from 'react';
import { Copy, Download, Check } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../Dialog';
import { Button } from '../../Button';
import { EXPORT_CONFIG, type ExportResponse } from '@/lib/api/export';

interface ExportModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  data: ExportResponse | null;
  isLoading?: boolean;
}

export const ExportModal = ({
  open,
  onOpenChange,
  data,
  isLoading = false,
}: ExportModalProps) => {
  const [copied, setCopied] = useState(false);

  const config = data ? EXPORT_CONFIG[data.format] : null;

  const handleCopy = async () => {
    if (!data?.content) return;

    try {
      await navigator.clipboard.writeText(data.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  const handleDownload = () => {
    if (!data?.content || !config) return;

    const blob = new Blob([data.content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${data.schemaName || 'schema'}${config.extension}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>{config?.title ?? 'Export'}</DialogTitle>
        </DialogHeader>

        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-schemafy-text" />
          </div>
        ) : data ? (
          <>
            <div className="flex items-center justify-between text-sm text-schemafy-dark-gray mb-2">
              <span>
                {data.schemaName} · {data.tableCount} tables
              </span>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleCopy}
                  className="gap-1"
                >
                  {copied ? (
                    <Check className="w-4 h-4" />
                  ) : (
                    <Copy className="w-4 h-4" />
                  )}
                  {copied ? 'Copied' : 'Copy'}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleDownload}
                  className="gap-1"
                >
                  <Download className="w-4 h-4" />
                  Download
                </Button>
              </div>
            </div>

            <div className="flex-1 overflow-auto rounded-lg bg-schemafy-secondary border border-schemafy-dark-gray/20">
              <pre className="p-4 text-sm text-schemafy-text font-mono whitespace-pre overflow-x-auto">
                {data.content}
              </pre>
            </div>
          </>
        ) : (
          <div className="flex items-center justify-center py-12 text-schemafy-dark-gray">
            No data available
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};
