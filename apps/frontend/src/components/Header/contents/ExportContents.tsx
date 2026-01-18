import { useState } from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';
import { RadioGroup, RadioGroupItem } from '../../RadioGroup';

export const ExportContents = () => {
  const [exportFormat, setExportFormat] = useState<string>('DDL');

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={'none'} size={'none'}>
          Export
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="flex flex-col gap-2.5">
        <RadioGroup value={exportFormat} onValueChange={setExportFormat}>
          <RadioGroupItem value={'DDL'}>DDL Script</RadioGroupItem>
          <RadioGroupItem value={'Mermaid'}>Mermaid Code</RadioGroupItem>
          <RadioGroupItem value={'PNG'}>PNG</RadioGroupItem>
          <RadioGroupItem value={'SVG'}>SVG</RadioGroupItem>
        </RadioGroup>
        <Button fullWidth>Export</Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
