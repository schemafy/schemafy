import { Bot, Ellipsis, HelpCircle } from 'lucide-react';
import { useState } from 'react';

interface FloatingButtonsProps {
  isShortcutPanelOpen: boolean;
  onHelpClick: () => void;
}

export const FloatingButtons = ({
  isShortcutPanelOpen,
  onHelpClick,
}: FloatingButtonsProps) => {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="fixed bottom-4 left-4 z-50 sm:bottom-6 sm:left-6">
      <div
        className="schemafy-canvas-panel flex min-h-12 min-w-12 flex-col items-center rounded-full p-1.5 transition-all duration-300 ease-in-out"
        onMouseEnter={() => setIsExpanded(true)}
        onMouseLeave={() => setIsExpanded(false)}
      >
        <div
          className={`flex flex-col-reverse gap-2 transition-all duration-300 ${
            isExpanded
              ? 'max-h-40 opacity-100'
              : 'max-h-0 opacity-0 overflow-hidden'
          }`}
        >
          <button
            onClick={onHelpClick}
            className="schemafy-focus-ring flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-schemafy-secondary"
          >
            <HelpCircle
              size={18}
              color={
                isShortcutPanelOpen
                  ? 'var(--color-schemafy-text)'
                  : 'var(--color-schemafy-dark-gray)'
              }
            />
          </button>

          <button className="schemafy-focus-ring flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-schemafy-secondary">
            <Bot size={18} color="var(--color-schemafy-dark-gray)" />
          </button>
        </div>

        <button className="schemafy-focus-ring flex h-9 w-9 items-center justify-center rounded-full">
          <Ellipsis size={18} color="var(--color-schemafy-dark-gray)" />
        </button>
      </div>
    </div>
  );
};
