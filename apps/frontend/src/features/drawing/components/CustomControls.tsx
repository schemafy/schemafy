import { useReactFlow } from '@xyflow/react';
import { Plus, Minus } from 'lucide-react';

export const CustomControls = () => {
  const { zoomIn, zoomOut } = useReactFlow();
  return (
    <div
      style={{
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
      }}
      className="z-10 bg-schemafy-bg text-schemafy-text px-5 py-3 gap-5 flex rounded-lg absolute bottom-45 right-4"
    >
      <button onClick={() => zoomIn()}>
        <Plus size={16} color="var(--color-schemafy-dark-gray)" />
      </button>
      <button onClick={() => zoomOut()}>
        <Minus size={16} color="var(--color-schemafy-dark-gray)" />
      </button>
    </div>
  );
};
