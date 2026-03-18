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
    <div className="fixed bottom-4 left-4 z-50">
      <div
        className="flex flex-col items-center bg-schemafy-bg rounded-full shadow-lg p-2 transition-all duration-300 ease-in-out"
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
            className="flex items-center justify-center p-4 rounded-full hover:bg-schemafy-secondary transition-colors"
          >
            <HelpCircle
              size={24}
              color={
                isShortcutPanelOpen
                  ? 'var(--color-schemafy-text)'
                  : 'var(--color-schemafy-dark-gray)'
              }
            />
          </button>

          <button className="flex items-center justify-center p-4 rounded-full hover:bg-schemafy-secondary transition-colors">
            <Bot size={24} color="var(--color-schemafy-dark-gray)" />
          </button>
        </div>

        <button className="flex items-center p-4 rounded-full justify-center">
          <Ellipsis size={24} color="var(--color-schemafy-dark-gray)" />
        </button>
      </div>
    </div>
  );
};