import { useEffect, useState } from 'react';
import type { ChatMessage } from '@/features/collaboration/api';

const FADE_OUT_START = 3500;

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
      className="schemafy-strong-panel mt-1 max-w-xs overflow-hidden rounded-2xl"
      style={{
        opacity,
        transition: 'opacity 500ms',
        borderColor: 'hsl(var(--schemafy-glass-border))',
        boxShadow: `0 0 0 1px ${color}22, var(--shadow-schemafy-float)`,
      }}
    >
      <div
        className="whitespace-nowrap px-2.5 py-1 font-overline-xs text-white"
        style={{ backgroundColor: color }}
      >
        {message.userName}
      </div>
      <div className="break-words px-2.5 py-1.5 font-body-sm text-schemafy-text">
        {message.content}
      </div>
    </div>
  );
};
