import { collaborationStore } from '@/store';
import { useEffect } from 'react';

const DISPLAY_DURATION = 4000;

export const useChatMessages = () => {
  useEffect(() => {
    const timerMap = new Map<string, ReturnType<typeof setTimeout>>();

    const unsubscribe = collaborationStore.onChatMessage((message) => {
      const cursor = collaborationStore.cursors.get(message.sessionId);
      const position =
        message.position ?? (cursor ? { x: cursor.x, y: cursor.y } : undefined);

      collaborationStore.setActiveChatMessage(message.sessionId, {
        ...message,
        position,
      });

      if (timerMap.has(message.sessionId)) {
        clearTimeout(timerMap.get(message.sessionId)!);
      }

      const timer = setTimeout(() => {
        collaborationStore.clearActiveChatMessage(message.sessionId);
        timerMap.delete(message.sessionId);
      }, DISPLAY_DURATION);

      timerMap.set(message.sessionId, timer);
    });

    return () => {
      unsubscribe();
      timerMap.forEach(clearTimeout);
    };
  }, []);
};
