import {
  Hand,
  MessageCircle,
  MessageCircleMore,
  MousePointer2,
  Table,
  X,
} from 'lucide-react';

const SHORTCUTS = [
  { icon: MousePointer2, name: 'Pointer', key: 'p' },
  { icon: Hand, name: 'Hand', key: 'h' },
  { icon: Table, name: 'Add Entity', key: 'e' },
  { icon: MessageCircleMore, name: 'Add Memo', key: 'm' },
  { icon: MessageCircle, name: 'Chat message', key: '/' },
];

interface ShortcutPanelProps {
  onClose: () => void;
}

export const ShortcutPanel = ({ onClose }: ShortcutPanelProps) => {
  return (
    <div
      className="fixed inset-0 z-40"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="schemafy-canvas-panel absolute bottom-6 left-28 min-w-[320px] rounded-2xl px-6 py-4">
        <div className="flex items-center justify-between mb-3">
          <span className="font-body-sm font-semibold text-schemafy-text">
            Keyboard Shortcuts
          </span>
          <button
            onClick={onClose}
            className="schemafy-focus-ring rounded-lg p-1 transition-colors hover:bg-schemafy-secondary"
          >
            <X size={14} color="var(--color-schemafy-dark-gray)" />
          </button>
        </div>

        <div className="flex flex-col gap-2">
          {SHORTCUTS.map(({ icon: Icon, name, key }) => (
            <div key={name} className="flex items-center justify-between gap-8">
              <div className="flex items-center gap-2">
                <Icon size={14} color="var(--color-schemafy-dark-gray)" />
                <span className="font-body-sm text-schemafy-text">{name}</span>
              </div>
              <kbd className="rounded-md border border-schemafy-glass-border bg-schemafy-secondary/80 px-2 py-0.5 font-mono text-xs text-schemafy-dark-gray">
                {key}
              </kbd>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
