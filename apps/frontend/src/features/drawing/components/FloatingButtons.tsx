import { Bot, MessageCircle, Ellipsis } from 'lucide-react';
import { useState } from 'react';

export const FloatingButtons = () => {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="fixed bottom-3 left-3">
      <div
        className="flex flex-col items-center bg-schemafy-bg rounded-full shadow-lg p-2 transition-all duration-300 ease-in-out"
        onMouseEnter={() => setIsExpanded(true)}
        onMouseLeave={() => setIsExpanded(false)}
      >
        <div
          className={`flex flex-col-reverse gap-2 transition-all duration-300 ${
            isExpanded ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0 overflow-hidden'
          }`}
        >
          <button className="flex items-center justify-center p-4 rounded-full hover:bg-schemafy-secondary transition-colors">
            <MessageCircle size={24} color="var(--color-schemafy-dark-gray)" />
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
