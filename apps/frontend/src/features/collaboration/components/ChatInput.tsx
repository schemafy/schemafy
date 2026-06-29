import { useEffect, useRef, useState } from 'react';
import { observer } from 'mobx-react-lite';
import {
  CURSOR_TRANSITION_MS,
  getCursorColor,
} from '@/features/collaboration/utils';
import { collaborationStore } from '@/store/collaboration.store';
import { useInactivityTimer } from '@/features/collaboration/hooks/useInactivityTimer';
import type { ChatMessage } from '@/features/collaboration/api';

const INACTIVITY_CLOSE_MS = 3500;
const MESSAGE_FADE_OUT_START_MS = 3500;

const SLIDE_STYLES = `
  @keyframes chat-message-in {
    from { transform: translateY(120%); opacity: 0; }
    to   { transform: translateY(0);    opacity: 1; }
  }
  @keyframes chat-container-in {
    from { transform: scale(0.9); opacity: 0; }
    to   { transform: scale(1);   opacity: 1; }
  }
  @keyframes chat-container-out {
    from { transform: scale(1);   opacity: 1; }
    to   { transform: scale(0.9); opacity: 0; }
  }
`;

const ANIMATION_MS = 250;
const CONTAINER_ANIMATION_MS = 200;
const FADE_OUT_MS = 500;

interface ChatInputProps {
  position: { x: number; y: number };
  isExiting: boolean;
  onSend: (message: string) => void;
  onCancel: () => void;
}

interface ChatMessageItemProps {
  message: ChatMessage;
}

const ChatMessageItem = ({ message }: ChatMessageItemProps) => {
  const [isFadingOut, setIsFadingOut] = useState(false);

  useEffect(() => {
    setIsFadingOut(false);
    const timer = setTimeout(
      () => setIsFadingOut(true),
      MESSAGE_FADE_OUT_START_MS,
    );

    return () => clearTimeout(timer);
  }, [message.messageId]);

  return (
    <div
      className="flex min-h-8 items-center rounded-xl px-3 py-1.5 font-body-sm text-schemafy-text"
      style={{
        animation: `chat-message-in ${ANIMATION_MS}ms ease forwards`,
        opacity: isFadingOut ? 0 : 1,
        transition: `opacity ${FADE_OUT_MS}ms ease`,
      }}
    >
      {message.content}
    </div>
  );
};

export const ChatInput = observer(
  ({ position, isExiting, onSend, onCancel }: ChatInputProps) => {
    const [message, setMessage] = useState('');
    const inputRef = useRef<HTMLInputElement>(null);

    const sessionId = collaborationStore.sessionId ?? '';
    const activeMessages =
      collaborationStore.activeChatMessages.get(sessionId) ?? [];
    const color = getCursorColor(sessionId);

    const resetInactivityTimer = useInactivityTimer(
      onCancel,
      INACTIVITY_CLOSE_MS,
    );

    useEffect(() => {
      inputRef.current?.focus();
    }, []);

    if (!collaborationStore.sessionId) return null;

    const handleKeyDown = (e: React.KeyboardEvent) => {
      resetInactivityTimer();
      if (e.key === 'Enter' && !e.shiftKey) {
        if (e.nativeEvent.isComposing) return;
        e.preventDefault();
        if (message.trim()) {
          onSend(message.trim());
          setMessage('');
        }
      } else if (e.key === 'Escape') {
        e.preventDefault();
        onCancel();
      }
    };

    const hasMessages = activeMessages.length > 0;

    return (
      <>
        <style>{SLIDE_STYLES}</style>
        <div
          data-chat-input
          className="fixed z-50 pointer-events-auto"
          style={{
            left: 0,
            top: 0,
            transform: `translate3d(${position.x}px, ${position.y}px, 0)`,
            transition: `transform ${CURSOR_TRANSITION_MS}ms linear`,
          }}
        >
          <div
            className="schemafy-strong-panel relative min-w-[220px] max-w-sm rounded-2xl p-1.5"
            style={{
              borderColor: 'hsl(var(--schemafy-glass-border))',
              boxShadow: `0 0 0 1px ${color}22, var(--shadow-schemafy-float)`,
              animation: isExiting
                ? `chat-container-out ${CONTAINER_ANIMATION_MS}ms ease forwards`
                : `chat-container-in ${CONTAINER_ANIMATION_MS}ms ease forwards`,
            }}
          >
            {hasMessages && (
              <div className="schemafy-strong-panel absolute bottom-full left-0 right-0 mb-2 flex max-h-24 flex-col overflow-hidden rounded-2xl p-1">
                {activeMessages.map((activeMessage) => (
                  <ChatMessageItem
                    key={activeMessage.messageId}
                    message={activeMessage}
                  />
                ))}
              </div>
            )}
            <input
              ref={inputRef}
              type="text"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="메시지 입력..."
              className="schemafy-focus-ring w-full rounded-xl bg-schemafy-secondary/85 px-3 py-2 font-body-sm text-schemafy-text placeholder:text-schemafy-dark-gray"
              maxLength={500}
            />
          </div>
        </div>
      </>
    );
  },
);
