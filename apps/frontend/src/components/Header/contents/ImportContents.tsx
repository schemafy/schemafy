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
        <Button
          variant={'none'}
          size={'none'}
          className="schemafy-menu-button px-3 py-2"
        >
          Import
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="flex flex-col gap-3">
        <div className="flex flex-col gap-0.5">
          <span className="font-overline-xs text-schemafy-text">Import</span>
          <span className="font-caption-sm text-schemafy-dark-gray">
            Upload schema resources.
          </span>
        </div>
        <div>
          <input
            type="file"
            id="file-upload"
            className="hidden"
            onChange={() => {}}
          />
          <label
            htmlFor="file-upload"
            className="flex w-full cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-schemafy-glass-border px-2.5 py-5 font-caption-sm text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary"
          >
            <FilePlus size={16} />
            File Upload
          </label>
        </div>
        <Button fullWidth size={'dropdown'}>
          Import
        </Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
