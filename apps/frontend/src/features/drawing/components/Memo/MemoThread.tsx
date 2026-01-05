import type { MemoComment } from '@/lib/api';
import { useMemoContext } from '../../context';
import { CircleCheck, MoveUp, Trash, X } from 'lucide-react';
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
            <CircleCheck size={14} color="var(--color-schemafy-dark-gray)" />
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
          <ReplyItem key={comment.id} memoId={id} comment={comment} />
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
  );
};
