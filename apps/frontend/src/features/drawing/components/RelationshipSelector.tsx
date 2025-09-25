import {
  type RelationshipConfig,
  type RelationshipType,
  RELATIONSHIP_TYPES,
} from '../types';

interface RelationshipSelectorProps {
  config: RelationshipConfig;
  onChange: (config: RelationshipConfig) => void;
}

export const RelationshipSelector = ({
  config,
  onChange,
}: RelationshipSelectorProps) => {
  const handleTypeChange = (type: RelationshipType) => {
    onChange({ ...config, type });
  };

  const handleOptionalChange = (isDashed: boolean) => {
    onChange({ ...config, isDashed });
  };

  return (
    <div className="bg-schemafy-bg border border-schemafy-light-gray rounded-lg p-3 min-w-48">
      <div className="text-xs font-medium text-schemafy-text mb-3">
        Relationship Type
      </div>

      <div className="space-y-2 mb-3">
        {Object.entries(RELATIONSHIP_TYPES).map(([key, typeConfig]) => (
          <button
            key={key}
            onClick={() => handleTypeChange(key as RelationshipType)}
            className={`
              w-full text-left px-2 py-1 rounded text-sm hover:bg-schemafy-secondary flex items-center gap-2
              ${config.type === key ? 'bg-schemafy-secondary border border-schemafy-light-gray' : ''}
            `}
          >
            <LinePreview
              type={key as RelationshipType}
              isDashed={config.isDashed}
            />
            {typeConfig.label}
          </button>
        ))}
      </div>

      <div className="border-t border-schemafy-light-gray my-3"></div>

      <label className="flex items-center gap-2 cursor-pointer text-sm">
        <input
          type="checkbox"
          checked={config.isDashed}
          onChange={(e) => handleOptionalChange(e.target.checked)}
          className="w-4 h-4 text-schemafy-text rounded focus:ring-schemafy-text"
        />
        <span className="text-schemafy-text">Dashed</span>
      </label>
    </div>
  );
};

const LinePreview = ({
  isDashed,
}: {
  type: RelationshipType;
  isDashed: boolean;
}) => {
  const color = 'var(--color-schemafy-dark-gray)';

  return (
    <svg width="24" height="4" className="flex-shrink-0">
      <line
        x1="0"
        y1="2"
        x2="24"
        y2="2"
        stroke={color}
        strokeWidth="2"
        strokeDasharray={isDashed ? '5 5' : 'none'}
      />
    </svg>
  );
};
