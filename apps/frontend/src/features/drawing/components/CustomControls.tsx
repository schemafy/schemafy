import { useReactFlow } from '@xyflow/react';
import { Plus, Minus } from 'lucide-react';

export const CustomControls = () => {
  const { zoomIn, zoomOut } = useReactFlow();
  return (
    <div className="schemafy-canvas-controls schemafy-canvas-panel absolute right-6 bottom-45 z-10 flex h-12 items-center gap-1 rounded-2xl px-2 text-schemafy-text">
      <button
        onClick={() => zoomIn()}
        className="schemafy-focus-ring flex h-9 w-9 items-center justify-center rounded-xl text-schemafy-dark-gray transition-all duration-200 hover:bg-schemafy-secondary hover:text-schemafy-text"
        aria-label="Zoom in"
      >
        <Plus size={16} />
      </button>
      <button
        onClick={() => zoomOut()}
        className="schemafy-focus-ring flex h-9 w-9 items-center justify-center rounded-xl text-schemafy-dark-gray transition-all duration-200 hover:bg-schemafy-secondary hover:text-schemafy-text"
        aria-label="Zoom out"
      >
        <Minus size={16} />
      </button>
    </div>
  );
};
