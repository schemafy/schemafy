import { Button } from '../Button';
import { Avatar } from '../Avatar';
import { ImportContents } from './contents/ImportContents';
import { ExportContents } from './contents/ExportContents';
import { ShareContents } from './contents/ShareContents';
import { VersionsContents } from './contents/VersionContents';
import { SettingsContents } from './contents/SettingsContents';

export const CanvasHeader = () => {
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
