import { useState, useEffect, useRef } from 'react';

interface ChatInputProps {
  position: { x: number; y: number };
  onSend: (message: string) => void;
  onCancel: () => void;
}

export const ChatInput = ({ position, onSend, onCancel }: ChatInputProps) => {
  const [message, setMessage] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
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

  return (
    <div
      className="fixed z-50 pointer-events-auto"
      style={{
        left: 0,
        top: 0,
        transform: `translate3d(${position.x}px, ${position.y}px, 0)`,
      }}
    >
      <div className="bg-schemafy-bg border border-schemafy-light-gray rounded-lg shadow-lg p-2 min-w-[300px]">
        <input
          ref={inputRef}
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="메시지 입력"
          className="w-full px-3 py-2 bg-schemafy-secondary text-schemafy-text border border-schemafy-light-gray rounded font-body-sm focus:outline-none focus:ring-2 focus:ring-schemafy-blue"
          maxLength={500}
        />
        <div className="flex justify-between items-center mt-1 px-1">
          <span className="font-caption-sm text-schemafy-dark-gray">
            {message.length}/500
          </span>
          <span className="font-caption-sm text-schemafy-dark-gray">
            Enter: 전송 | Esc: 취소
          </span>
        </div>
      </div>
    </div>
  );
};
