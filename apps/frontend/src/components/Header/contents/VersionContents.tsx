import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';
import { Avatar } from '../../Avatar';

export const VersionsContents = () => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant={'none'}
          size={'none'}
          className="schemafy-menu-button px-3 py-2"
        >
          Versions
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex min-w-[18rem] flex-col gap-3 font-body-xs"
      >
        <div className="flex flex-col gap-0.5">
          <span className="font-overline-xs text-schemafy-text">
            Version activity
          </span>
          <span className="font-caption-sm text-schemafy-dark-gray">
            Recent schema updates.
          </span>
        </div>
        <div className="flex flex-col gap-1.5 rounded-xl border border-schemafy-glass-border bg-schemafy-panel px-3 py-2">
          <div className="flex gap-2.5 items-center">
            <Avatar
              size={'dropdown'}
              src="https://picsum.photos/200/300?random=1"
            />
            <p>user1 updates schema [User]</p>
          </div>
          <span className="font-caption-sm text-schemafy-dark-gray">
            2025.08.25. 01:35
          </span>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
