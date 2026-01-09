import { useShareLinkStore } from '@/hooks';
import type { ShareLinkRole } from '@/lib/api';
import { useState } from 'react';
import { useLocation } from 'react-router-dom';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '../Dropdown';
import { Button } from '../Button';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../Select';
import { Link } from 'lucide-react';

export const ShareContents = () => {
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
