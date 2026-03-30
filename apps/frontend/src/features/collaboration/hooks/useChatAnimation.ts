import { useEffect, useRef, useState } from 'react';
import type { ChatMessage } from '@/features/collaboration/api';

const ANIMATION_MS = 250;
const MESSAGE_DURATION_MS = 4000;

interface ChatAnimationState {
  displayedMessage: ChatMessage | null;
  exitingMessage: ChatMessage | null;
  isEntering: boolean;
}

export const useChatAnimation = (
  activeMessage: ChatMessage | undefined,
): ChatAnimationState => {
  const [displayedMessage, setDisplayedMessage] = useState<ChatMessage | null>(
    null,
  );
  const [exitingMessage, setExitingMessage] = useState<ChatMessage | null>(
    null,
  );
  const [isEntering, setIsEntering] = useState(false);

  const addedIds = useRef<Set<string>>(new Set());
  const displayedRef = useRef<ChatMessage | null>(null);
  const removeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const exitAnimTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const setDisplayed = (msg: ChatMessage | null) => {
    displayedRef.current = msg;
    setDisplayedMessage(msg);
  };

  useEffect(() => {
    return () => {
      if (removeTimerRef.current) clearTimeout(removeTimerRef.current);
      if (exitAnimTimerRef.current) clearTimeout(exitAnimTimerRef.current);
    };
  }, []);

  useEffect(() => {
    if (!activeMessage) return;
    if (addedIds.current.has(activeMessage.messageId)) return;
    addedIds.current.add(activeMessage.messageId);

    if (removeTimerRef.current) clearTimeout(removeTimerRef.current);
    if (exitAnimTimerRef.current) clearTimeout(exitAnimTimerRef.current);

    setExitingMessage(displayedRef.current);
    setDisplayed(activeMessage);
    setIsEntering(true);

    exitAnimTimerRef.current = setTimeout(() => {
      setExitingMessage(null);
      setIsEntering(false);
    }, ANIMATION_MS);

    removeTimerRef.current = setTimeout(() => {
      setExitingMessage(displayedRef.current);
      setDisplayed(null);
      exitAnimTimerRef.current = setTimeout(
        () => setExitingMessage(null),
        ANIMATION_MS,
      );
    }, MESSAGE_DURATION_MS);
  }, [activeMessage?.messageId]);

  return { displayedMessage, exitingMessage, isEntering };
};
