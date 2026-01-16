import { useEffect, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { CollaborationStore } from '@/store';
import type { ChatMessage } from '@/lib/api/collaboration';

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
    }, 2500);

    const expireTimer = setTimeout(() => {
      onExpire();
    }, 3000);

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
        left: message.position.x,
        top: message.position.y,
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
  const collaborationStore = CollaborationStore.getInstance();
  const [displayMessages, setDisplayMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    const latestMessage =
      collaborationStore.messages[collaborationStore.messages.length - 1];

    if (!latestMessage) return;

    const cursor = collaborationStore.cursors.get(latestMessage.userName);

    if (!cursor) return;

    const messageWithPosition: ChatMessage = {
      ...latestMessage,
      position: { x: cursor.x + 20, y: cursor.y + 20 },
    };

    setDisplayMessages((prev) => [...prev, messageWithPosition]);
  }, [collaborationStore.messages.length, collaborationStore]);

  const handleExpire = (messageId: string) => {
    setDisplayMessages((prev) =>
      prev.filter((msg) => msg.messageId !== messageId),
    );
  };

  return (
    <>
      {displayMessages.map((message) => (
        <FloatingChatMessage
          key={message.messageId}
          message={message}
          onExpire={() => handleExpire(message.messageId)}
        />
      ))}
    </>
  );
});
