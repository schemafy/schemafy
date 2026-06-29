import type { MemoComment } from '@/features/memo/api/types';
import { Avatar } from '@/components';
import { useMemoContext } from '../hooks/useMemoStore';
import { formatDate } from '@/lib';
import { Pencil, Trash } from 'lucide-react';
import { useState } from 'react';

export const ReplyItem = ({
  memoId,
  comment,
}: {
  memoId: string;
  comment: MemoComment;
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editInput, setEditInput] = useState(comment.body);

  const { updateComment, deleteComment } = useMemoContext();

  const handleSave = () => {
    if (!editInput.trim()) return;
    updateComment(memoId, comment.id, editInput.trim());
    setIsEditing(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.nativeEvent.isComposing) return;
    if (e.key === 'Enter') {
      handleSave();
    }
    if (e.key === 'Escape') {
      setIsEditing(false);
      setEditInput(comment.body);
    }
  };

  return (
    <li className="group flex w-full items-start gap-3 rounded-xl border border-transparent px-3 py-2.5 text-schemafy-text transition-colors duration-200 hover:border-schemafy-glass-border hover:bg-schemafy-secondary/50">
      <div className="shrink-0 pt-0.5">
        <Avatar
          size={'dropdown'}
          src="https://picsum.photos/200/300?random=1"
        />
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex min-h-7 items-start justify-between gap-3">
          <div className="min-w-0 flex flex-wrap items-center gap-x-2 gap-y-1 pr-2">
            <span className="truncate font-overline-sm">
              {comment.author.name}
            </span>
            <span className="font-body-xs text-schemafy-dark-gray">
              {formatDate(
                new Date(comment.updatedAt ?? comment.createdAt ?? new Date()),
              )}
            </span>
          </div>
          {!isEditing && (
            <div className="flex shrink-0 gap-1 opacity-0 transition-opacity group-focus-within:opacity-100 group-hover:opacity-100">
              <button
                type="button"
                title="Edit Reply"
                onClick={() => {
                  setIsEditing(true);
                  setEditInput(comment.body);
                }}
                className="schemafy-icon-button schemafy-focus-ring flex h-7 w-7 items-center justify-center"
              >
                <Pencil size={14} />
              </button>
              <button
                type="button"
                title="Delete Reply"
                onClick={() => deleteComment(memoId, comment.id)}
                className="schemafy-icon-button schemafy-focus-ring flex h-7 w-7 items-center justify-center hover:text-schemafy-destructive"
              >
                <Trash size={14} />
              </button>
            </div>
          )}
        </div>

        {isEditing ? (
          <div className="mt-2 flex flex-col gap-2.5">
            <input
              value={editInput}
              onChange={(e) => setEditInput(e.target.value)}
              onKeyDown={handleKeyDown}
              className="schemafy-input w-full px-3 py-2 font-body-sm"
              autoFocus
            />
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsEditing(false)}
                className="schemafy-menu-button schemafy-focus-ring px-3 py-1.5 font-body-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSave}
                className="schemafy-focus-ring rounded-full bg-schemafy-button-bg px-3 py-1.5 font-body-xs font-medium text-schemafy-button-text transition-colors duration-200"
              >
                Save
              </button>
            </div>
          </div>
        ) : (
          <p className="mt-1 break-words font-body-sm leading-relaxed">
            {comment.body}
          </p>
        )}
      </div>
    </li>
  );
};
