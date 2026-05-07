import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';
import { Avatar } from '../../Avatar';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../Select';

const RoleSelect = () => {
  return (
    <Select>
      <SelectTrigger className="w-[3.75rem] border-none font-body-xs">
        <SelectValue placeholder="admin" />
      </SelectTrigger>
      <SelectContent popover="manual">
        <SelectGroup>
          <SelectItem value="admin">admin</SelectItem>
          <SelectItem value="viewer">viewer</SelectItem>
          <SelectItem value="editor">editor</SelectItem>
        </SelectGroup>
      </SelectContent>
    </Select>
  );
};

export const ShareContents = () => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={'none'} size={'none'}>
          Share
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex flex-col gap-2.5 font-body-xs"
      >
        <div className="flex gap-4">
          <input
            type="email"
            className="w-[12.5rem] placeholder:text-schemafy-dark-gray bg-secondary rounded-[10px] px-3 py-2"
            placeholder="schemafy@email.com"
          />
          <RoleSelect />
          <Button size={'dropdown'}>Invite</Button>
        </div>
        <p className="text-schemafy-dark-gray">Who has access</p>
        <div className="flex justify-between items-center">
          <div className="flex gap-2.5 items-center">
            <Avatar
              size={'dropdown'}
              src="https://picsum.photos/200/300?random=1"
            />
            <p>name</p>
          </div>
          <RoleSelect />
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
