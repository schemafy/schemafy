import { useEffect, useRef, useState } from 'react';
import { Download } from 'lucide-react';
import * as DropdownMenuPrimitive from '@radix-ui/react-dropdown-menu';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';
import {
  ExportModal,
  type ExportFormat,
  type SchemaExportRequest,
  useSchemaExport,
} from '@/features/export';
import { getApiErrorMessage } from '@/lib/api';

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
  const controllerRef = useRef<AbortController | null>(null);
  const pendingRequestRef = useRef<SchemaExportRequest | null>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const exportMutation = useSchemaExport();

  useEffect(
    () => () => {
      controllerRef.current?.abort();
      controllerRef.current = null;
    },
    [],
  );

  const startExport = (exportRequest: SchemaExportRequest) => {
    if (exportMutation.isPending) return;

    const controller = new AbortController();

    controllerRef.current?.abort();
    controllerRef.current = controller;
    setRequest(exportRequest);
    setIsModalOpen(true);
    exportMutation.mutate({
      schemaId: exportRequest.schemaId,
      format: exportRequest.format,
      signal: controller.signal,
    });
  };

  const handleExport = () => {
    pendingRequestRef.current = { schemaId, schemaName, format: exportFormat };
    setIsDropdownOpen(false);
  };

  const handleRetry = () => {
    if (request) startExport(request);
  };

  const closeModal = () => {
    controllerRef.current?.abort();
    controllerRef.current = null;
    exportMutation.reset();
    setIsModalOpen(false);
    setRequest(null);
  };

  return (
    <>
      <DropdownMenu open={isDropdownOpen} onOpenChange={setIsDropdownOpen}>
        <DropdownMenuTrigger asChild>
          <Button
            ref={triggerRef}
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
          onCloseAutoFocus={(event) => {
            const pendingRequest = pendingRequestRef.current;
            if (!pendingRequest) return;
            event.preventDefault();
            pendingRequestRef.current = null;
            startExport(pendingRequest);
          }}
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
          <DropdownMenuPrimitive.RadioGroup
            value={exportFormat}
            onValueChange={(value) => setExportFormat(value as ExportFormat)}
          >
            {(['ddl', 'mermaid'] as const).map((format) => (
              <DropdownMenuPrimitive.RadioItem
                key={format}
                value={format}
                onSelect={(event) => event.preventDefault()}
                className="schemafy-focus-ring flex cursor-pointer items-center gap-2 rounded-xl px-3 py-2 font-body-xs text-schemafy-text outline-none data-[highlighted]:bg-schemafy-secondary"
              >
                <span
                  aria-hidden="true"
                  className="flex h-3 w-3 items-center justify-center rounded-full border border-current"
                >
                  <DropdownMenuPrimitive.ItemIndicator className="h-1.5 w-1.5 rounded-full bg-current" />
                </span>
                {format === 'ddl' ? 'MySQL DDL' : 'Mermaid'}
              </DropdownMenuPrimitive.RadioItem>
            ))}
          </DropdownMenuPrimitive.RadioGroup>
          <DropdownMenuPrimitive.Item asChild onSelect={handleExport}>
            <Button
              fullWidth
              size="dropdown"
              disabled={exportMutation.isPending}
            >
              Export
            </Button>
          </DropdownMenuPrimitive.Item>
        </DropdownMenuContent>
      </DropdownMenu>

      <ExportModal
        open={isModalOpen}
        onOpenChange={(open) => {
          if (!open) closeModal();
        }}
        request={request}
        result={exportMutation.data ?? null}
        status={exportMutation.isPending ? 'loading' : exportMutation.status}
        errorMessage={
          exportMutation.error ? getApiErrorMessage(exportMutation.error) : null
        }
        onRetry={handleRetry}
        onCloseAutoFocus={(event) => {
          event.preventDefault();
          if (!isDropdownOpen) triggerRef.current?.focus();
        }}
      />
    </>
  );
};
