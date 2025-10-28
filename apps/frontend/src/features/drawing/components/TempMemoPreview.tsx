import { useState, useRef, useEffect } from 'react';
import { Avatar } from '@/components';
import { MoveUp } from 'lucide-react';

interface TempMemoPreviewProps {
  position: { x: number; y: number };
  onConfirm: (content: string) => void;
  onCancel: () => void;
}

export const TempMemoPreview = ({ position, onConfirm, onCancel }: TempMemoPreviewProps) => {
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
      className="absolute z-[1000] flex gap-4 items-center text-schemafy-text"
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`,
        transform: 'translate(0, -50%)',
      }}
      onKeyDown={handleKeyDown}
    >
      <div className="memo-icon w-[32px] h-[32px] rounded-t-full rounded-br-full bg-schemafy-bg flex justify-center items-center shadow-md">
        <Avatar size={'dropdown'} src="https://picsum.photos/200/300?random=1" />
      </div>

      <input
        type="text"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="Add a Memo"
        autoFocus
        className="px-4 py-2 placeholder:text-schemafy-dark-gray text-schemafy-text font-body-sm rounded-lg outline-none focus:outline-none shadow-md bg-schemafy-bg"
      />

      <button
        onClick={handleCreate}
        className="w-8 h-8 flex justify-center items-center bg-schemafy-button-bg rounded-full shadow-md"
      >
        <MoveUp size={14} color="var(--color-schemafy-button-text)" />
      </button>
    </div>
  );
};
