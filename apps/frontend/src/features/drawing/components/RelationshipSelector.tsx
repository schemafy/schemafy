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

  const handleNonIdentifyingChange = (isNonIdentifying: boolean) => {
    onChange({ ...config, isNonIdentifying });
  };

  return (
    <div className="bg-schemafy-bg border border-schemafy-light-gray rounded-lg p-3 min-w-48">
      <div className="text-xs font-medium text-schemafy-text mb-3">
        Relationship Type
      </div>

      <div className="space-y-2 mb-3">
        {Object.entries(RELATIONSHIP_TYPES).map(([key, typeInfo]) => (
          <button
            key={key}
            onClick={() => handleTypeChange(key as RelationshipType)}
            className={`
              w-full text-left px-2 py-1 rounded text-sm text-schemafy-text hover:bg-schemafy-secondary flex items-center gap-2
              ${config.type === key ? 'bg-schemafy-secondary border border-schemafy-light-gray' : ''}
            `}
          >
            <LinePreview
              isNonIdentifying={config.isNonIdentifying}
              type={key as RelationshipType}
            />
            {typeInfo.label}
          </button>
        ))}
      </div>

      <div className="border-t border-schemafy-light-gray my-3"></div>

      <label className="flex items-center gap-2 cursor-pointer text-sm mb-3">
        <input
          type="checkbox"
          checked={config.isNonIdentifying}
          onChange={(e) => handleNonIdentifyingChange(e.target.checked)}
          className="w-4 h-4 text-schemafy-text rounded focus:ring-schemafy-text"
        />
        <span className="text-schemafy-text">Non-Identifying</span>
      </label>
    </div>
  );
};

const LinePreview = ({
  isNonIdentifying,
  type,
}: {
  isNonIdentifying: boolean;
  type: RelationshipType;
}) => {
  const color = 'var(--color-schemafy-dark-gray)';
  const isMany = type === 'one-to-many';
  const lineEnd = isMany ? 16 : 24;

  return (
    <svg width="24" height="12" className="flex-shrink-0">
      <line
        x1="0"
        y1="6"
        x2={lineEnd}
        y2="6"
        stroke={color}
        strokeWidth="2"
        strokeDasharray={isNonIdentifying ? '5 5' : 'none'}
      />
      {isMany && (
        <>
          <line x1="24" y1="1" x2="16" y2="6" stroke={color} strokeWidth="2" />
          <line x1="24" y1="6" x2="16" y2="6" stroke={color} strokeWidth="2" />
          <line x1="24" y1="11" x2="16" y2="6" stroke={color} strokeWidth="2" />
        </>
      )}
    </svg>
  );
};
