import { useEffect, useState } from 'react';
import type { ChatMessage } from '@/features/collaboration/api';

const FADE_OUT_START = 2500;

interface ChatBubbleProps {
  message: ChatMessage;
  color: string;
}

export const ChatBubble = ({ message, color }: ChatBubbleProps) => {
  const [opacity, setOpacity] = useState(1);

  useEffect(() => {
    setOpacity(1);
    const timer = setTimeout(() => setOpacity(0), FADE_OUT_START);
    return () => clearTimeout(timer);
  }, [message.messageId]);

  return (
    <div
      className="mt-0.5 overflow-hidden rounded-lg shadow-lg max-w-xs"
      style={{ opacity, transition: 'opacity 500ms' }}
    >
      <div
        className="px-1.5 py-0.5 font-overline-xs text-white whitespace-nowrap"
        style={{ backgroundColor: color }}
      >
        {message.userName}
      </div>
      <div
        className="px-1.5 py-0.5 font-body-sm text-white break-words"
        style={{ backgroundColor: color }}
      >
        {message.content}
      </div>
    </div>
  );
};
