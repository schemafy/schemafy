import { useState, useContext } from 'react';
import { useLocation } from 'react-router-dom';
import { Avatar } from '../Avatar';
import { Button } from '../Button';
import { RadioGroup, RadioGroupItem } from '../RadioGroup';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../Dropdown';
import { FilePlus, Link } from 'lucide-react';
import { ThemeProviderContext, type Theme } from '@/lib/config';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../Select';
import { AuthStore } from '@/store';
import { useShareLinkStore } from '@/hooks';
import type { ShareLinkRole } from '@/lib/api/shareLink/types';

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
  const user = AuthStore.getInstance().user;

  return (
    <div className="flex items-center justify-between w-full">
      <div className="flex items-center gap-9 ml-8">
        <Button variant={'none'} size={'none'}>
          Product
        </Button>
        <Button variant={'none'} size={'none'}>
          Settings
        </Button>
        <Button variant={'none'} size={'none'}>
          Notifications
        </Button>
      </div>
      <div className="flex items-center gap-4">
        <div className="flex gap-2">
          <Button round to="/projects">
            New Project
          </Button>
          <Button variant={'secondary'} round>
            Sign Out
          </Button>
        </div>
        <span className="flex items-center font-body-sm text-schemafy-dark-gray">
          {user?.name}
        </span>
        <Avatar src="https://picsum.photos/200/300?random=1" />
      </div>
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
  const location = useLocation();
  const pathParts = location.pathname.split('/');
  const workspaceId = pathParts[2];
  const projectId = pathParts[3];

  const { createShareLink, generatedLink, isLoading } = useShareLinkStore();
  const [selectedRole, setSelectedRole] = useState<ShareLinkRole>('viewer');
  const [copySuccess, setCopySuccess] = useState(false);

  const handleGenerateLink = async () => {
    console.log(workspaceId, projectId);
    if (!workspaceId || !projectId) return;

    setCopySuccess(false);

    await createShareLink(workspaceId, projectId, selectedRole);
  };

  const handleCopyLink = async () => {
    if (!generatedLink) return;

    try {
      await navigator.clipboard.writeText(generatedLink);
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    } catch {
      console.error('Failed to copy link');
    }
  };

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
        <p className="font-overline-xs text-schemafy-dark-gray">
          Generate Share Link
        </p>

        <div className="flex flex-col gap-2">
          <label className="font-caption-sm text-schemafy-text">Role</label>
          <Select
            value={selectedRole}
            onValueChange={(value) => setSelectedRole(value as ShareLinkRole)}
          >
            <SelectTrigger className="w-full border border-schemafy-light-gray font-body-xs">
              <SelectValue placeholder="Select role" />
            </SelectTrigger>
            <SelectContent popover="manual">
              <SelectGroup>
                <SelectItem value="viewer">Viewer</SelectItem>
                <SelectItem value="commenter">Commenter</SelectItem>
                <SelectItem value="editor">Editor</SelectItem>
              </SelectGroup>
            </SelectContent>
          </Select>
        </div>

        <Button
          fullWidth
          size={'dropdown'}
          onClick={handleGenerateLink}
          disabled={isLoading}
        >
          {isLoading ? 'Generating...' : 'Generate Link'}
        </Button>

        {generatedLink && (
          <div className="flex flex-col gap-2 pt-2 border-t border-schemafy-light-gray">
            <p className="font-caption-sm text-schemafy-dark-gray">
              Share Link
            </p>
            <div className="flex gap-2 items-center">
              <input
                type="text"
                readOnly
                value={generatedLink}
                className="flex-1 placeholder:text-schemafy-dark-gray bg-secondary rounded-[10px] px-3 py-2 text-xs truncate"
              />
              <Button
                size={'dropdown'}
                variant={copySuccess ? 'secondary' : 'default'}
                onClick={handleCopyLink}
              >
                <Link size={14} className="mr-1" />
                {copySuccess ? 'Copied!' : 'Copy'}
              </Button>
            </div>
          </div>
        )}
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

const SettingsContents = () => {
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
