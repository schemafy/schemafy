import { useContext } from 'react';
import { ThemeProviderContext, type Theme } from '@/lib/config';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';
import { RadioGroup, RadioGroupItem } from '../../RadioGroup';

export const SettingsContents = () => {
  const { theme, setTheme } = useContext(ThemeProviderContext);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={'none'} size={'none'}>
          Settings
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex flex-col gap-2.5 font-body-xs"
      >
        <div className="flex justify-between gap-4 items-center">
          <label className="font-overline-xs">Name</label>
          <input
            type="text"
            value={'Schemafy'}
            onChange={() => {}}
            className="w-[12.5rem] placeholder:text-schemafy-dark-gray bg-secondary rounded-[10px] px-3 py-2"
          />
        </div>
        <div className="flex justify-between gap-4 items-center">
          <label className="font-overline-xs">Description</label>
          <input
            type="text"
            value={'Schemafy ERD Diagram'}
            onChange={() => {}}
            className="w-[12.5rem] placeholder:text-schemafy-dark-gray bg-secondary rounded-[10px] px-3 py-2"
          />
        </div>
        <div className="flex gap-4 items-center">
          <label className="font-overline-xs">Theme</label>
          <RadioGroup
            value={theme}
            onValueChange={(value) => setTheme(value as Theme)}
            className="flex-row"
          >
            <RadioGroupItem value={'light'}>Light</RadioGroupItem>
            <RadioGroupItem value={'dark'}>Dark</RadioGroupItem>
            <RadioGroupItem value={'system'}>System</RadioGroupItem>
          </RadioGroup>
        </div>
        <Button fullWidth size={'dropdown'}>
          Save
        </Button>
        <Button fullWidth size={'dropdown'} variant={'outline'}>
          Delete Project
        </Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
