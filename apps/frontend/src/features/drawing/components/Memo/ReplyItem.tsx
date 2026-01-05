import type { MemoComment } from '@/lib/api';
import { Avatar } from '@/components';
import { useMemoContext } from '../../context/MemoContext';
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
    <li className="flex gap-2 text-schemafy-text group">
      <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1" />
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <span className="font-overline-sm">{comment.author.name}</span>
          <span className="font-body-xs text-schemafy-dark-gray">
            {formatDate(
              new Date(comment.updatedAt ?? comment.createdAt ?? new Date()),
            )}
          </span>
          {!isEditing && (
            <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <Pencil
                size={14}
                color="var(--color-schemafy-dark-gray)"
                onClick={() => {
                  setIsEditing(true);
                  setEditInput(comment.body);
                }}
                className="cursor-pointer hover:text-schemafy-text"
              />
              <Trash
                size={14}
                color="var(--color-schemafy-dark-gray)"
                onClick={() => deleteComment(memoId, comment.id)}
                className="cursor-pointer hover:text-red-500"
              />
            </div>
          )}
        </div>

        {isEditing ? (
          <div className="mt-1 flex flex-col gap-2">
            <input
              value={editInput}
              onChange={(e) => setEditInput(e.target.value)}
              onKeyDown={handleKeyDown}
              className="w-full bg-schemafy-secondary px-2 py-1 text-schemafy-text font-body-sm rounded outline-none border border-schemafy-blue"
              autoFocus
            />
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => setIsEditing(false)}
                className="text-xs text-schemafy-dark-gray hover:text-schemafy-text"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                className="text-xs text-schemafy-blue hover:text-blue-400 font-medium"
              >
                Save
              </button>
            </div>
          </div>
        ) : (
          <p className="font-body-sm mt-1">{comment.body}</p>
        )}
      </div>
    </li>
  );
};
