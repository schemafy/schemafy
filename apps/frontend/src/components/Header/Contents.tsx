import { useState, useContext } from 'react';
import { Avatar } from '../Avatar';
import { Button } from '../Button';
import { RadioGroup, RadioGroupItem } from '../RadioGroup';
import { DropdownMenu, DropdownMenuContent, DropdownMenuTrigger } from '../DropDown';
import { FilePlus, Link } from 'lucide-react';
import { ThemeProviderContext, type Theme } from '@/lib/config';

export const LandingContents = () => {
  return (
    <div className="flex items-center gap-2">
      <Button round to="/signup">
        Get Started
      </Button>
      <Button variant={'secondary'} round to="/signin">
        Sign In
      </Button>
    </div>
  );
};

export const DashBoardContents = () => {
  return (
    <div className="flex items-center gap-9">
      <Button variant={'none'} size={'none'}>
        Projects
      </Button>
      <Button variant={'none'} size={'none'}>
        Settings
      </Button>
      <Button variant={'none'} size={'none'}>
        Notifications
      </Button>
      <div className="flex gap-2">
        <Button round>New Project</Button>
        <Button variant={'secondary'} round to="/signin">
          Sign Out
        </Button>
      </div>
      <Avatar src="https://picsum.photos/200/300?random=1" />
    </div>
  );
};

export const CanvasContents = () => {
  return (
    <div className="flex items-center gap-9">
      <ImportContents />
      <ExportContents />
      <ShareContents />
      <VersionsContents />
      <SettingsContents />
      <Button variant={'secondary'} round>
        Sign Out
      </Button>
      <div className="flex items-center gap-2">
        <div className="flex items-center -space-x-3 *:data-[slot=avatar]:ring-background *:data-[slot=avatar]:ring-2 [&>*:nth-child(1)]:z-30 [&>*:nth-child(2)]:z-20 [&>*:nth-child(3)]:z-10">
          <Avatar src="https://picsum.photos/200/300?random=1" />
          <Avatar src="https://picsum.photos/100/300?random=1" deactivate />
          <Avatar src="https://picsum.photos/200/100?random=1" deactivate />
        </div>
        <span className="font-overline-xs text-schemafy-dark-gray">+ 3</span>
      </div>
    </div>
  );
};

const ImportContents = () => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={'none'} size={'none'}>
          Import
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="flex flex-col gap-2.5">
        <div>
          <input type="file" id="file-upload" className="hidden" onChange={() => {}} />
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

const ExportContents = () => {
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

const ShareContents = () => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={'none'} size={'none'}>
          Share
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="flex flex-col gap-2.5 font-body-xs">
        <div className="flex gap-4">
          <input
            type="email"
            className="w-[12.5rem] placeholder:text-schemafy-dark-gray bg-secondary rounded-[10px] px-3 py-2"
            placeholder="schemafy@email.com"
          />
          <Button size={'dropdown'}>Invite</Button>
        </div>
        <div className="flex gap-1 justify-end items-center hover:text-blue-500">
          <Link size={10} />
          <Button variant={'none'} size={'none'} className="text-schemafy-dark-gray font-body-xs">
            Copy Link
          </Button>
        </div>
        <p className="text-schemafy-dark-gray">Who has access</p>
        <div className="flex justify-between items-center">
          <div className="flex gap-2.5 items-center">
            <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1" />
            <p>name</p>
          </div>
          {/* selector */}
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

const VersionsContents = () => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={'none'} size={'none'}>
          Versions
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="flex flex-col gap-2.5 font-body-xs">
        <div className="flex flex-col gap-1.5">
          <div className="flex gap-2.5 items-center">
            <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1" />
            <p>user1 updates schema [User]</p>
          </div>
          <span className="font-caption-sm text-schemafy-dark-gray">2025.08.25. 01:35</span>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

const SettingsContents = () => {
  const { theme, setTheme } = useContext(ThemeProviderContext);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={'none'} size={'none'}>
          Settings
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="flex flex-col gap-2.5 font-body-xs">
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
          <RadioGroup value={theme} onValueChange={(value) => setTheme(value as Theme)} className="flex-row">
            <RadioGroupItem value={'light'}>Light</RadioGroupItem>
            <RadioGroupItem value={'dark'}>Dark</RadioGroupItem>
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
