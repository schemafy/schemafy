import { useEffect, useState } from 'react';
import { observer } from 'mobx-react-lite';
import type { ChatMessage } from '@/lib/api/collaboration';
import { useChatMessages } from '@/hooks';

const DISPLAY_DURATION = 3000;
const FADE_OUT_START = 2500;

interface FloatingChatMessageProps {
  message: ChatMessage;
  onExpire: () => void;
}

const FloatingChatMessage = ({
  message,
  onExpire,
}: FloatingChatMessageProps) => {
  const [opacity, setOpacity] = useState(1);

  useEffect(() => {
    const fadeTimer = setTimeout(() => {
      setOpacity(0);
    }, FADE_OUT_START);

    const expireTimer = setTimeout(() => {
      onExpire();
    }, DISPLAY_DURATION);

    return () => {
      clearTimeout(fadeTimer);
      clearTimeout(expireTimer);
    };
  }, [onExpire]);

  if (!message.position) return null;

  return (
    <div
      className="fixed pointer-events-none z-50 transition-opacity duration-500"
      style={{
        left: 0,
        top: 0,
        transform: `translate3d(${message.position.x}px, ${message.position.y}px, 0)`,
        opacity,
      }}
    >
      <div className="bg-schemafy-bg border border-schemafy-light-gray rounded-lg shadow-lg px-3 py-2 max-w-xs">
        <div className="font-overline-xs text-schemafy-dark-gray mb-1">
          {message.userName}
        </div>
        <div className="font-body-sm text-schemafy-text break-words">
          {message.content}
        </div>
      </div>
    </div>
  );
};

export const ChatOverlay = observer(() => {
  const { displayMessages, removeMessage } = useChatMessages();

  return (
    <>
      {displayMessages.map((message) => (
        <FloatingChatMessage
          key={message.messageId}
          message={message}
          onExpire={() => removeMessage(message.messageId)}
        />
      ))}
    </>
  );
});
