import { Avatar } from '@/components';
import { formatDate } from '@/lib';
import type { MemoComment } from '@/features/memo/api/types';

interface MemoHoverPreviewProps {
  firstComment: MemoComment;
  comments: MemoComment[];
}

export const MemoHoverPreview = ({
  firstComment,
  comments,
}: MemoHoverPreviewProps) => {
  return (
    <div className="schemafy-canvas-panel pointer-events-none absolute left-full top-0 z-10 ml-2 min-w-[220px] max-w-[300px] animate-in rounded-2xl p-3 fade-in duration-150">
      <div className="flex gap-2">
        <Avatar
          size={'dropdown'}
          src="https://picsum.photos/200/300?random=1"
        />
        <div className="flex-1 overflow-hidden">
          <div className="flex items-center gap-2">
            <span className="font-overline-sm truncate">
              {firstComment.author.name}
            </span>
            <span className="font-body-xs text-schemafy-dark-gray shrink-0">
              {formatDate(
                new Date(
                  firstComment.updatedAt ??
                    firstComment.createdAt ??
                    new Date(),
                ),
              )}
            </span>
          </div>
          <p className="mt-1 line-clamp-2 font-body-sm text-schemafy-text">
            {firstComment.body}
          </p>
        </div>
      </div>
      {comments.length > 1 && (
        <p className="font-body-xs text-schemafy-dark-gray mt-2 text-right">
          +{comments.length - 1} more
        </p>
      )}
    </div>
  );
};
