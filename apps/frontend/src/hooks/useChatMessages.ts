import { CollaborationStore } from '@/store';
import type { ChatMessage } from '@/lib/api/collaboration';
import { useEffect, useState } from 'react';

export const useChatMessages = () => {
  const collaborationStore = CollaborationStore.getInstance();
  const [displayMessages, setDisplayMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    const unsubscribe = collaborationStore.onChatMessage((message) => {
      if (message.position) {
        setDisplayMessages((prev) => {
          if (prev.some((msg) => msg.messageId === message.messageId)) {
            return prev;
          }
          return [...prev, message];
        });
        return;
      }

      const cursor = collaborationStore.cursors.get(message.userId);

      if (!cursor) {
        console.error('Cursor not found');
        return;
      }

      const messageWithPosition: ChatMessage = {
        ...message,
        position: { x: cursor.x + 20, y: cursor.y + 20 },
      };

      setDisplayMessages((prev) => {
        if (prev.some((msg) => msg.messageId === message.messageId)) {
          return prev;
        }
        return [...prev, messageWithPosition];
      });
    });

    return unsubscribe;
  }, [collaborationStore]);

  const removeMessage = (messageId: string) => {
    setDisplayMessages((prev) =>
      prev.filter((msg) => msg.messageId !== messageId),
    );
  };

  return { displayMessages, removeMessage };
};
