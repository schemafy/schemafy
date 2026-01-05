import { Avatar } from "@/components";
import { formatDate } from "@/lib";
import type { MemoComment } from "@/lib/api";

interface MemoHoverPreviewProps {
  firstComment: MemoComment;
  comments: MemoComment[];
}

export const MemoHoverPreview = ({ firstComment, comments }: MemoHoverPreviewProps) => {
  return (
    <div className="absolute left-full top-0 ml-2 bg-schemafy-bg rounded-lg shadow-lg p-3 min-w-[200px] max-w-[280px] pointer-events-none z-10 animate-in fade-in duration-150">
      <div className="flex gap-2">
        <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1" />
        <div className="flex-1 overflow-hidden">
          <div className="flex items-center gap-2">
            <span className="font-overline-sm truncate">{firstComment.author.name}</span>
            <span className="font-body-xs text-schemafy-dark-gray shrink-0">
              {formatDate(new Date(firstComment.updatedAt ?? firstComment.createdAt ?? new Date()))}
            </span>
          </div>
          <p className="font-body-sm mt-1 line-clamp-2">{firstComment.body}</p>
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