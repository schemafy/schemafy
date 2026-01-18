import { FilePlus } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';

export const ImportContents = () => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={'none'} size={'none'}>
          Import
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="flex flex-col gap-2.5">
        <div>
          <input
            type="file"
            id="file-upload"
            className="hidden"
            onChange={() => {}}
          />
          <label
            htmlFor="file-upload"
            className="flex flex-col gap-2 px-2.5 py-4 items-center justify-center w-full border border-dashed border-schemafy-dark-gray font-caption-sm text-schemafy-dark-gray rounded-lg cursor-pointer hover:bg-schemafy-secondary transition-opacity"
          >
            <FilePlus size={16} />
            File Upload
          </label>
        </div>
        <Button fullWidth>Import</Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
