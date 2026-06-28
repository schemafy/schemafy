import { collaborationStore } from '@/store';
import { useEffect } from 'react';

const DISPLAY_DURATION = 4000;

export const useChatMessages = () => {
  useEffect(() => {
    const timerMap = new Map<string, ReturnType<typeof setTimeout>>();

    const unsubscribe = collaborationStore.onChatMessage((message) => {
      const timerKey = `${message.sessionId}:${message.messageId}`;
      const cursor = collaborationStore.cursors.get(message.sessionId);
      const position =
        message.position ?? (cursor ? { x: cursor.x, y: cursor.y } : undefined);

      collaborationStore.setActiveChatMessage(message.sessionId, {
        ...message,
        position,
      });

      if (timerMap.has(timerKey)) {
        clearTimeout(timerMap.get(timerKey)!);
      }

      const timer = setTimeout(() => {
        collaborationStore.clearActiveChatMessage(
          message.sessionId,
          message.messageId,
        );
        timerMap.delete(timerKey);
      }, DISPLAY_DURATION);

      timerMap.set(timerKey, timer);
    });

    return () => {
      unsubscribe();
      timerMap.forEach(clearTimeout);
    };
  }, []);
};
