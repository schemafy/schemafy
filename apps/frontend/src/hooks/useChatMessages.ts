import { collaborationStore } from '@/store';
import type { ChatMessage } from '@/lib/api/collaboration';
import { useEffect, useState } from 'react';

export const useChatMessages = () => {
  const [displayMessages, setDisplayMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    const unsubscribe = collaborationStore.onChatMessage((message) => {
      const cursor = collaborationStore.cursors.get(message.userId);

      if (!cursor) {
        console.error('Cursor not found');
        return;
      }

      const messageWithPosition: ChatMessage = {
        ...message,
        position: { x: cursor.x + 20, y: cursor.y + 20 },
      };

      setDisplayMessages((prev) => [...prev, messageWithPosition]);
    });

    return unsubscribe;
  }, []);

  const removeMessage = (messageId: string) => {
    setDisplayMessages((prev) =>
      prev.filter((msg) => msg.messageId !== messageId),
    );
  };

  return { displayMessages, removeMessage };
};
