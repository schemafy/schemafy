import { useState, useRef, useEffect } from 'react';
import { Avatar } from '@/components';
import { MoveUp } from 'lucide-react';
import type { Point } from '../types';

interface TempMemoPreviewProps {
  position: Point;
  onConfirm: (content: string) => void;
  onCancel: () => void;
}

export const TempMemoPreview = ({
  position,
  onConfirm,
  onCancel,
}: TempMemoPreviewProps) => {
  const [content, setContent] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);

  const handleCreate = () => {
    if (content.trim()) {
      onConfirm(content);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleCreate();
    } else if (e.key === 'Escape') {
      onCancel();
    }
  };

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement;

      if (containerRef.current?.contains(target)) {
        return;
      }

      if (target.classList.contains('react-flow__pane')) {
        onCancel();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [onCancel]);

  return (
    <div
      ref={containerRef}
      className="absolute z-[1000] flex items-center gap-3 text-schemafy-text"
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`,
        transform: 'translate(0, -50%)',
      }}
      onKeyDown={handleKeyDown}
    >
      <div className="memo-icon schemafy-canvas-panel flex h-9 w-9 items-center justify-center rounded-t-full rounded-br-full">
        <Avatar
          size={'dropdown'}
          src="https://picsum.photos/200/300?random=1"
        />
      </div>

      <input
        type="text"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="Add a Memo"
        autoFocus
        className="schemafy-input px-4 py-2 font-body-sm shadow-md"
      />

      <button
        onClick={handleCreate}
        className="schemafy-focus-ring flex h-8 w-8 items-center justify-center rounded-full bg-schemafy-button-bg shadow-md transition-colors duration-200"
      >
        <MoveUp size={14} color="var(--color-schemafy-button-text)" />
      </button>
    </div>
  );
};
