import { useEffect, useRef, useState } from 'react';
import { Download } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';
import { RadioGroup, RadioGroupItem } from '../../RadioGroup';
import {
  ExportModal,
  type ExportFormat,
  type ExportStatus,
  type SchemaExportRequest,
  type SchemaExportResult,
  useSchemaExport,
} from '@/features/export';

interface ExportContentsProps {
  schemaId: string;
  schemaName: string;
}

export const ExportContents = ({
  schemaId,
  schemaName,
}: ExportContentsProps) => {
  const [exportFormat, setExportFormat] = useState<ExportFormat>('ddl');
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [request, setRequest] = useState<SchemaExportRequest | null>(null);
  const [result, setResult] = useState<SchemaExportResult | null>(null);
  const [status, setStatus] = useState<ExportStatus>('idle');
  const controllerRef = useRef<AbortController | null>(null);
  const exportMutation = useSchemaExport();

  useEffect(
    () => () => {
      controllerRef.current?.abort();
      controllerRef.current = null;
    },
    [],
  );

  const executeExport = async (
    exportRequest: SchemaExportRequest,
    controller: AbortController,
  ) => {
    try {
      const exportResult = await exportMutation.mutateAsync({
        schemaId: exportRequest.schemaId,
        format: exportRequest.format,
        signal: controller.signal,
      });

      if (controllerRef.current !== controller) return;
      setResult(exportResult);
      setStatus('success');
    } catch {
      if (controllerRef.current !== controller || controller.signal.aborted) {
        return;
      }
      setStatus('error');
    }
  };

  const handleExport = () => {
    if (status === 'loading') return;

    const exportRequest: SchemaExportRequest = {
      schemaId,
      schemaName,
      format: exportFormat,
    };
    const controller = new AbortController();

    controllerRef.current?.abort();
    controllerRef.current = controller;
    setRequest(exportRequest);
    setResult(null);
    setStatus('loading');
    setIsDropdownOpen(false);
    setIsModalOpen(true);
    void executeExport(exportRequest, controller);
  };

  const closeModal = () => {
    controllerRef.current?.abort();
    controllerRef.current = null;
    exportMutation.reset();
    setIsModalOpen(false);
    setRequest(null);
    setResult(null);
    setStatus('idle');
  };

  return (
    <>
      <DropdownMenu open={isDropdownOpen} onOpenChange={setIsDropdownOpen}>
        <DropdownMenuTrigger asChild>
          <Button
            variant={'none'}
            size={'none'}
            className="schemafy-menu-button schemafy-header-button flex h-9 w-9 items-center justify-center p-0 sm:h-auto sm:w-auto sm:px-3 sm:py-2"
            aria-label="Export"
          >
            <Download className="h-4 w-4 sm:hidden" />
            <span className="hidden sm:inline">Export</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent
          align="end"
          className="flex w-[calc(100vw-2rem)] min-w-0 flex-col gap-3 sm:w-auto"
        >
          <div className="flex flex-col gap-0.5">
            <span className="font-overline-xs text-schemafy-text">
              Export as
            </span>
            <span className="font-caption-sm text-schemafy-dark-gray">
              Choose a format for the current schema.
            </span>
          </div>
          <RadioGroup
            value={exportFormat}
            onValueChange={(value) => setExportFormat(value as ExportFormat)}
          >
            <RadioGroupItem value="ddl">MySQL DDL</RadioGroupItem>
            <RadioGroupItem value="mermaid">Mermaid</RadioGroupItem>
          </RadioGroup>
          <Button
            fullWidth
            size={'dropdown'}
            disabled={status === 'loading'}
            onClick={handleExport}
          >
            Export
          </Button>
        </DropdownMenuContent>
      </DropdownMenu>

      <ExportModal
        open={isModalOpen}
        onOpenChange={(open) => {
          if (!open) closeModal();
        }}
        request={request}
        result={result}
        status={status}
      />
    </>
  );
};
