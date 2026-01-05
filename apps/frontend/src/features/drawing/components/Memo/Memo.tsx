import { useState } from 'react';
import { Avatar } from '@/components';
import { cn } from '@/lib';
import { CircleCheck, MoveUp, Trash, X } from 'lucide-react';
import type { MemoData } from '../../hooks/memo.helper';
import { observer } from 'mobx-react-lite';
import { useMemoContext } from '../../context/MemoContext';
import { ReplyItem } from './ReplyItem';
import { MemoHoverPreview } from './MemoHoverPreview';

interface MemoProps {
  id: string;
  data: MemoData;
}

export const Memo = observer(({ id, data }: MemoProps) => {
  const [showThread, setShowThread] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const [replyInput, setReplyInput] = useState('');

  const { deleteMemo, createComment } = useMemoContext();

  const comments = data.comments;
  console.log(comments);
  const firstComment = comments ? comments[0] : null;

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

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.nativeEvent.isComposing) return;
    if (e.key === 'Enter') {
      handleAddReply();
    }
  };

  const showHoverPreview = isHovered && !showThread && firstComment;

  return (
    <div className="flex gap-4 text-schemafy-text">
      <div className="relative">
        <div
          className={cn(
            'memo-icon w-[32px] h-[32px] rounded-t-full rounded-br-full bg-schemafy-bg flex justify-center items-center shadow-md cursor-pointer',
            showThread && 'ring-2 ring-schemafy-blue',
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

        {showHoverPreview && <MemoHoverPreview firstComment={firstComment} comments={comments} />}
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
      )}
    </div>
  );
});
