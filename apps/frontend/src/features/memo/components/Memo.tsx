import { useState } from 'react';
import { Avatar } from '@/components';
import { cn } from '@/lib';
import type { MemoData } from '../hooks/memo.helper';
import { MemoHoverPreview } from './MemoHoverPreview';
import { MemoThread } from './MemoThread';

interface MemoProps {
  id: string;
  data: MemoData;
}

export const Memo = ({ id, data }: MemoProps) => {
  const [showThread, setShowThread] = useState(false);
  const [isHovered, setIsHovered] = useState(false);

  const comments = data.comments;
  const firstComment = comments ? comments[0] : null;

  const handleIconClick = () => {
    setShowThread(!showThread);
  };

  const showHoverPreview = isHovered && !showThread && firstComment;

  return (
    <div className="flex gap-4 text-schemafy-text">
      <div className="relative">
        <div
          className={cn(
            'memo-icon schemafy-canvas-panel flex h-9 w-9 cursor-pointer items-center justify-center rounded-t-full rounded-br-full transition-all duration-200',
            showThread && 'ring-2 ring-schemafy-soft-blue',
          )}
          onClick={handleIconClick}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => setIsHovered(false)}
        >
          <Avatar
            size={'dropdown'}
            src="https://picsum.photos/200/300?random=1"
          />
        </div>

        {showHoverPreview && (
          <MemoHoverPreview firstComment={firstComment} comments={comments} />
        )}
      </div>

      {showThread && (
        <MemoThread id={id} comments={comments} setShowThread={setShowThread} />
      )}
    </div>
  );
};
