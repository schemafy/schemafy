import { useStore } from '@xyflow/react';
import { useState } from 'react';
import { Avatar } from '@/components';
import { cn, formatDate } from '@/lib';
import { CircleCheck, MoveUp, Trash, X, Pencil } from 'lucide-react';
import type { MemoData } from '../hooks/memo.helper';
import type { MemoComment } from '@/lib/api/memo/types';
import type { Point } from '../types';
import { observer } from 'mobx-react-lite';
import { useMemoContext } from '../context/MemoContext';

interface MemoProps {
  id: string;
  data: MemoData;
}

interface MemoPreviewProps {
  mousePosition: Point | null;
}

const ReplyItem = ({
  comment,
  onDelete,
  onUpdate,
}: {
  comment: MemoComment;
  onDelete: () => void;
  onUpdate: (newBody: string) => void;
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editInput, setEditInput] = useState(comment.body);

  const handleSave = () => {
    if (!editInput.trim()) return;
    onUpdate(editInput.trim());
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
          <span className="font-overline-sm">{comment.authorId}</span>
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
                onClick={onDelete}
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

export const Memo = observer(({ id, data }: MemoProps) => {
  const [showThread, setShowThread] = useState(false);
  const [replyInput, setReplyInput] = useState('');

  const { deleteMemo, createComment, updateComment, deleteComment } =
    useMemoContext();

  const comments = data.comments ?? [];

  const handleDeleteMemo = async () => {
    await deleteMemo(id);
  };

  const handleIconClick = () => {
    setShowThread(!showThread);
  };

  const handleAddReply = async () => {
    if (!replyInput.trim()) return;
    await createComment(id, replyInput.trim());
    setReplyInput('');
  };

  const handleUpdateComment = async (commentId: string, newBody: string) => {
    await updateComment(id, commentId, newBody);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.nativeEvent.isComposing) return;
    if (e.key === 'Enter') {
      handleAddReply();
    }
  };

  const handleDeleteComment = (commentId: string) => {
    deleteComment(id, commentId);
  };

  return (
    <div className="flex gap-4 text-schemafy-text">
      <div
        className={cn(
          'memo-icon w-[32px] h-[32px] rounded-t-full rounded-br-full bg-schemafy-bg flex justify-center items-center shadow-md cursor-pointer',
          showThread && 'ring-2 ring-schemafy-blue',
        )}
        onClick={handleIconClick}
      >
        <Avatar
          size={'dropdown'}
          src="https://picsum.photos/200/300?random=1"
        />
      </div>

      {showThread && (
        <div className="bg-schemafy-bg rounded-lg shadow-lg p-4 min-w-[300px] text-schemafy-text flex-col gap-4 flex">
          <div className="flex justify-between items-center">
            <h3 className="font-heading-xs">Memo</h3>
            <div className="flex gap-2">
              <button
                title="Resolved Memo"
                onClick={handleDeleteMemo}
                className="text-schemafy-dark-gray hover:text-schemafy-text cursor-pointer transition-colors duration-200 hover:bg-schemafy-light-gray rounded-sm p-1"
              >
                <Trash size={14} color="var(--color-schemafy-dark-gray)" />
              </button>
              <button
                title="Resolved Memo"
                onClick={() => {}}
                className="text-schemafy-dark-gray hover:text-schemafy-text cursor-pointer transition-colors duration-200 hover:bg-schemafy-light-gray rounded-sm p-1"
              >
                <CircleCheck
                  size={14}
                  color="var(--color-schemafy-dark-gray)"
                />
              </button>
              <button
                onClick={() => setShowThread(false)}
                className="text-schemafy-dark-gray hover:text-schemafy-text cursor-pointer transition-colors duration-200 hover:bg-schemafy-light-gray rounded-sm p-1"
              >
                <X size={14} color="var(--color-schemafy-dark-gray)" />
              </button>
            </div>
          </div>

          <ul className="flex flex-col gap-4">
            {comments.map((comment) => (
              <ReplyItem
                key={comment.id}
                comment={comment}
                onDelete={() => handleDeleteComment(comment.id)}
                onUpdate={(newBody) => handleUpdateComment(comment.id, newBody)}
              />
            ))}
          </ul>

          <div className="flex gap-2 items-center justify-between">
            <Avatar
              size={'dropdown'}
              src="https://picsum.photos/200/300?random=1"
            />
            <input
              type="text"
              value={replyInput}
              onChange={(e) => setReplyInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Reply"
              className="flex-1 bg-schemafy-secondary px-4 py-2 placeholder:text-schemafy-dark-gray text-schemafy-text font-body-sm rounded-lg outline-none focus:outline-none"
            />
            <button
              onClick={handleAddReply}
              className="w-8 h-8 flex justify-center items-center bg-schemafy-button-bg rounded-full"
            >
              <MoveUp size={14} color="var(--color-schemafy-button-text)" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
});

export const MemoPreview = ({ mousePosition }: MemoPreviewProps) => {
  const zoom = useStore((state) => state.transform[2]);

  if (!mousePosition) return null;

  return (
    <div
      className="bg-schemafy-button-bg rounded-br-full rounded-t-full"
      style={{
        position: 'absolute',
        pointerEvents: 'none',
        zIndex: 999,
        opacity: 0.4,
        width: `${24 * zoom}px`,
        height: `${24 * zoom}px`,
        transform: `translate(${mousePosition.x}px, ${mousePosition.y - 48}px)`,
      }}
    />
  );
};
