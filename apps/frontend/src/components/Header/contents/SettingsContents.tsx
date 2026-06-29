import { useContext } from 'react';
import { ThemeProviderContext, type Theme } from '@/lib/config';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../../DropDown';
import { Button } from '../../Button';
import { RadioGroup, RadioGroupItem } from '../../RadioGroup';
import { Settings } from 'lucide-react';

export const SettingsContents = () => {
  const { theme, setTheme } = useContext(ThemeProviderContext);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant={'none'}
          size={'none'}
          className="schemafy-menu-button schemafy-header-button flex h-9 w-9 items-center justify-center p-0 sm:h-auto sm:w-auto sm:px-3 sm:py-2"
          aria-label="Settings"
        >
          <Settings className="h-4 w-4 sm:hidden" />
          <span className="hidden sm:inline">Settings</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="flex w-[calc(100vw-2rem)] min-w-0 flex-col gap-3 font-body-xs sm:w-auto sm:min-w-[20rem]"
      >
        <div className="flex flex-col gap-0.5">
          <span className="font-overline-xs text-schemafy-text">
            Project settings
          </span>
          <span className="font-caption-sm text-schemafy-dark-gray">
            Adjust display preferences for the editor.
          </span>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
          <label className="font-overline-xs">Name</label>
          <input
            type="text"
            value={'Schemafy'}
            onChange={() => {}}
            className="schemafy-input w-full px-3 py-2 sm:w-[12.5rem]"
          />
        </div>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
          <label className="font-overline-xs">Description</label>
          <input
            type="text"
            value={'Schemafy ERD Diagram'}
            onChange={() => {}}
            className="schemafy-input w-full px-3 py-2 sm:w-[12.5rem]"
          />
        </div>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-4">
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
        <Button
          fullWidth
          size={'dropdown'}
          variant={'outline'}
          className="border-schemafy-destructive/40 text-schemafy-destructive hover:bg-schemafy-destructive/10"
        >
          Delete Project
        </Button>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
