import { Hand, MessageCircle, MessageCircleMore, MousePointer2, Table, X } from 'lucide-react';
import { MOD_KEY } from '@/lib/utils/modKey';

const SHORTCUTS = [
  {icon: MousePointer2, name: 'Pointer', key: `${MOD_KEY}1`},
  {icon: Hand, name: 'Hand', key: `${MOD_KEY}2`},
  {icon: Table, name: 'Add Entity', key: `${MOD_KEY}3`},
  {icon: MessageCircleMore, name: 'Add Memo', key: `${MOD_KEY}4`},
  {icon: MessageCircle, name: 'Chat message', key: '/'},
];

interface ShortcutPanelProps {
  onClose: () => void;
}

export const ShortcutPanel = ({onClose}: ShortcutPanelProps) => {
  return (
    <div
      className="fixed inset-0 z-40"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="absolute bottom-16 left-1/2 -translate-x-1/2 bg-schemafy-bg rounded-xl shadow-xl border border-schemafy-light-gray px-6 py-4 min-w-[320px]"
        style={{boxShadow: '0 8px 32px rgba(0,0,0,0.18)'}}
      >
        <div className="flex items-center justify-between mb-3">
          <span className="font-body-sm font-semibold text-schemafy-text">
            Keyboard Shortcuts
          </span>
          <button
            onClick={onClose}
            className="p-1 rounded hover:bg-schemafy-secondary transition-colors"
          >
            <X size={14} color="var(--color-schemafy-dark-gray)"/>
          </button>
        </div>

        <div className="flex flex-col gap-2">
          {SHORTCUTS.map(({icon: Icon, name, key}) => (
            <div key={name} className="flex items-center justify-between gap-8">
              <div className="flex items-center gap-2">
                <Icon size={14} color="var(--color-schemafy-dark-gray)"/>
                <span className="font-body-sm text-schemafy-text">{name}</span>
              </div>
              <kbd
                className="px-2 py-0.5 rounded bg-schemafy-secondary border border-schemafy-light-gray font-mono text-xs text-schemafy-dark-gray">
                {key}
              </kbd>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};