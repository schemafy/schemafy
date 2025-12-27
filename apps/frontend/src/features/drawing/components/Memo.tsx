import { useStore } from '@xyflow/react';
import { useState } from 'react';
import { Avatar } from '@/components';
import { cn, formatDate } from '@/lib';
import { CircleCheck, MoveUp, Trash, X } from 'lucide-react';
import type { MemoData } from '../hooks/memo.helper';
import type { MemoComment } from '@/lib/api/memo/types';
import type { Point } from '../types';
import { MemoStore } from '@/store';
import { observer } from 'mobx-react-lite';

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
}: {
  comment: MemoComment;
  onDelete: () => void;
}) => {
  return (
    <li className="flex gap-2 text-schemafy-text group">
      <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1" />
      <div>
        <div className="flex items-center gap-2">
          <span className="font-overline-sm">{comment.authorId}</span>
          <span className="font-body-xs text-schemafy-dark-gray">
            {formatDate(
              new Date(comment.updatedAt ?? comment.createdAt ?? new Date()),
            )}
          </span>
          <Trash
            size={14}
            color="var(--color-schemafy-dark-gray)"
            onClick={onDelete}
            className="cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
          />
        </div>
        <p className="font-body-sm mt-1">{comment.body}</p>
      </div>
    </li>
  );
};

export const Memo = observer(({ id, data }: MemoProps) => {
  const [showThread, setShowThread] = useState(false);
  const [replyInput, setReplyInput] = useState('');

  const memoStore = MemoStore.getInstance();

  const schemaId = Object.keys(memoStore.memosBySchema).find((sid) =>
    memoStore.memosBySchema[sid]?.some((m) => m.id === id),
  );

  const comments = data.comments ?? [];

  const handleDeleteMemo = async () => {
    if (schemaId) {
      await memoStore.deleteMemo(id, schemaId);
    }
  };

  const handleIconClick = () => {
    setShowThread(!showThread);
  };

  const handleAddReply = async () => {
    if (!replyInput.trim()) return;
    await memoStore.createMemoComment(id, { body: replyInput.trim() });
    setReplyInput('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.nativeEvent.isComposing) return;
    if (e.key === 'Enter') {
      handleAddReply();
    }
  };

  const handleDeleteComment = (commentId: string) => {
    memoStore.deleteMemoComment(id, commentId);
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

export const MemoPrivew = ({ mousePosition }: MemoPreviewProps) => {
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
