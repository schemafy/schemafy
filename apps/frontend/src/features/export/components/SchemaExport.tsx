import { useRef, useState } from 'react';
import { Download } from 'lucide-react';
import * as DropdownMenuPrimitive from '@radix-ui/react-dropdown-menu';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components/DropDown';
import { Button } from '@/components/Button';
import { type ExportFormat, type SchemaExportRequest } from '../api';
import { useSchemaExport } from '../hooks/useSchemaExport';
import { ExportModal } from './ExportModal';

interface SchemaExportProps {
  schemaId: string;
  schemaName: string;
}

export const SchemaExport = ({ schemaId, schemaName }: SchemaExportProps) => {
  const [exportFormat, setExportFormat] = useState<ExportFormat>('ddl');
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const pendingRequestRef = useRef<SchemaExportRequest | null>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const { state, isPending, startExport, retryExport, closeExport } =
    useSchemaExport();

  const handleExport = () => {
    pendingRequestRef.current = { schemaId, schemaName, format: exportFormat };
    setIsDropdownOpen(false);
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
            <Button fullWidth size="dropdown" disabled={isPending}>
              Export
            </Button>
          </DropdownMenuPrimitive.Item>
        </DropdownMenuContent>
      </DropdownMenu>

      <ExportModal
        state={state}
        onClose={closeExport}
        onRetry={retryExport}
        onCloseAutoFocus={(event) => {
          event.preventDefault();
          if (!isDropdownOpen) triggerRef.current?.focus();
        }}
      />
    </>
  );
};
