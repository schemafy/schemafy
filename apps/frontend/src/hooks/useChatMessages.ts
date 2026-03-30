import { collaborationStore } from '@/store';
import { useEffect } from 'react';

const DISPLAY_DURATION = 3000;

export const useChatMessages = () => {
  useEffect(() => {
    const unsubscribe = collaborationStore.onChatMessage((message) => {
      const cursor = collaborationStore.cursors.get(message.sessionId);
      const position =
        message.position ?? (cursor ? { x: cursor.x, y: cursor.y } : undefined);

      collaborationStore.setActiveChatMessage(message.sessionId, {
        ...message,
        position,
      });

      setTimeout(() => {
        collaborationStore.clearActiveChatMessage(message.sessionId);
      }, DISPLAY_DURATION);
    });

    return unsubscribe;
  }, []);
};
