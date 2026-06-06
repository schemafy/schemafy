import { useCallback, useEffect, useRef, useState } from 'react';
import { useThrottledCallback } from '@/hooks/useThrottledCallback';

const CHAT_EXIT_ANIMATION_MS = 200;
const CHAT_FOLLOW_THROTTLE_MS = 50;
const CHAT_OFFSET_PX = 16;

type Position = { x: number; y: number };

export const useChatInputAnchor = () => {
  const [position, setPosition] = useState<Position | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const exitTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (exitTimerRef.current) clearTimeout(exitTimerRef.current);
    };
  }, []);

  const close = useCallback(() => {
    if (exitTimerRef.current) return;
    setIsOpen(false);
    setIsExiting(true);
    exitTimerRef.current = setTimeout(() => {
      setPosition(null);
      setIsExiting(false);
      exitTimerRef.current = null;
    }, CHAT_EXIT_ANIMATION_MS);
  }, []);

  const open = useCallback((nextPosition: Position) => {
    if (exitTimerRef.current) {
      clearTimeout(exitTimerRef.current);
      exitTimerRef.current = null;
    }
    setIsExiting(false);
    setPosition(nextPosition);
    setIsOpen(true);
  }, []);

  const followMouse = useThrottledCallback(
    (clientX: number, clientY: number) => {
      setPosition({ x: clientX + CHAT_OFFSET_PX, y: clientY + CHAT_OFFSET_PX });
    },
    CHAT_FOLLOW_THROTTLE_MS,
  );

  useEffect(() => {
    if (!isOpen) return;

    const handleWindowMouseMove = (e: MouseEvent) => {
      followMouse(e.clientX, e.clientY);
    };

    const handleMouseLeave = () => {
      close();
    };

    const handleClickOutside = (e: MouseEvent) => {
      const chatInputEl = document.querySelector('[data-chat-input]');
      if (!chatInputEl || !chatInputEl.contains(e.target as Node)) {
        close();
      }
    };

    window.addEventListener('mousemove', handleWindowMouseMove);
    document.addEventListener('mouseleave', handleMouseLeave);
    window.addEventListener('mousedown', handleClickOutside, true);

    return () => {
      window.removeEventListener('mousemove', handleWindowMouseMove);
      document.removeEventListener('mouseleave', handleMouseLeave);
      window.removeEventListener('mousedown', handleClickOutside, true);
    };
  }, [isOpen, close, followMouse]);

  return { position, isOpen, isExiting, open, close };
};
