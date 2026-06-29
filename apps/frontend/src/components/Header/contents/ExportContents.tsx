import { useState } from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';
import { RadioGroup, RadioGroupItem } from '../../RadioGroup';
import { Download } from 'lucide-react';

export const ExportContents = () => {
  const [exportFormat, setExportFormat] = useState<string>('DDL');

  return (
    <DropdownMenu>
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
          <span className="font-overline-xs text-schemafy-text">Export as</span>
          <span className="font-caption-sm text-schemafy-dark-gray">
            Choose a format for the current schema.
          </span>
        </div>
        <RadioGroup value={exportFormat} onValueChange={setExportFormat}>
          <RadioGroupItem value={'DDL'}>DDL Script</RadioGroupItem>
          <RadioGroupItem value={'Mermaid'}>Mermaid Code</RadioGroupItem>
          <RadioGroupItem value={'PNG'}>PNG</RadioGroupItem>
          <RadioGroupItem value={'SVG'}>SVG</RadioGroupItem>
        </RadioGroup>
        <Button fullWidth size={'dropdown'}>
          Export
        </Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
