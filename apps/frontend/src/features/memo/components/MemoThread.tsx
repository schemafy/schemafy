import type { MemoComment } from '@/features/memo/api/types';
import { useMemoContext } from '../hooks/useMemoStore';
import { MoveUp, Trash, X } from 'lucide-react';
import { ReplyItem } from './ReplyItem';
import { Avatar } from '@/components';
import { useState } from 'react';

interface MemoThreadProps {
  id: string;
  comments: MemoComment[];
  setShowThread: (show: boolean) => void;
}

export const MemoThread = ({
  id,
  comments,
  setShowThread,
}: MemoThreadProps) => {
  const [replyInput, setReplyInput] = useState('');

  const { createComment, deleteMemo } = useMemoContext();

  const handleAddReply = async () => {
    if (!replyInput.trim()) return;
    await createComment(id, replyInput.trim());
    setReplyInput('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.nativeEvent.isComposing) return;
    if (e.key === 'Enter') {
      handleAddReply();
    }
  };

  const handleDeleteMemo = () => {
    deleteMemo(id);
  };

  return (
    <div className="schemafy-canvas-panel flex min-w-[320px] flex-col gap-4 rounded-2xl p-5 text-schemafy-text">
      <div className="flex items-center justify-between">
        <h3 className="font-heading-xs">Memo</h3>
        <div className="flex gap-2">
          <button
            type="button"
            title="Delete Memo"
            onClick={handleDeleteMemo}
            className="schemafy-icon-button schemafy-focus-ring flex h-8 w-8 items-center justify-center text-schemafy-dark-gray hover:text-schemafy-destructive"
          >
            <Trash size={14} />
          </button>
          <button
            type="button"
            onClick={() => setShowThread(false)}
            className="schemafy-icon-button schemafy-focus-ring flex h-8 w-8 items-center justify-center text-schemafy-dark-gray"
          >
            <X size={14} />
          </button>
        </div>
      </div>

      <ul className="schemafy-scrollbar flex max-h-72 flex-col gap-3 overflow-y-auto pr-1">
        {comments.map((comment) => (
          <ReplyItem key={comment.id} memoId={id} comment={comment} />
        ))}
      </ul>

      <div className="flex items-center justify-between gap-3 border-t border-schemafy-glass-border pt-4">
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
          className="schemafy-input flex-1 px-4 py-2 font-body-sm"
        />
        <button
          type="button"
          onClick={handleAddReply}
          className="schemafy-focus-ring flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-schemafy-button-bg transition-colors duration-200"
        >
          <MoveUp size={14} color="var(--color-schemafy-button-text)" />
        </button>
      </div>
    </div>
  );
};
