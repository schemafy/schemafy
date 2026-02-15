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
        <Button variant={'none'} size={'none'}>
          Versions
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex flex-col gap-2.5 font-body-xs"
      >
        <div className="flex flex-col gap-1.5">
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
