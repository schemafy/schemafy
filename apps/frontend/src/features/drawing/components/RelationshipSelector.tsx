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
    <div className="schemafy-canvas-panel min-w-48 rounded-2xl p-3">
      <div className="text-xs font-medium text-schemafy-text mb-3">
        Relationship Type
      </div>

      <div className="space-y-2 mb-3">
        {Object.entries(RELATIONSHIP_TYPES).map(([key, typeInfo]) => (
          <button
            key={key}
            onClick={() => handleTypeChange(key as RelationshipType)}
            className={`
              schemafy-focus-ring flex w-full items-center gap-2 rounded-xl px-2 py-1.5 text-left text-sm text-schemafy-text transition-colors hover:bg-schemafy-secondary
              ${config.type === key ? 'border border-schemafy-soft-blue/30 bg-schemafy-soft-blue/10' : ''}
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

      <div className="my-3 border-t border-schemafy-glass-border"></div>

      <label className="flex items-center gap-2 cursor-pointer text-sm mb-3">
        <input
          type="checkbox"
          checked={config.isNonIdentifying}
          onChange={(e) => handleNonIdentifyingChange(e.target.checked)}
          className="h-4 w-4 rounded text-schemafy-soft-blue focus:ring-schemafy-focus"
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
