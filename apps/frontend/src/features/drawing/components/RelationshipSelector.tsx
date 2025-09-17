import { type RelationshipType, RELATIONSHIP_TYPES } from '../types';

export const RelationshipSelector = ({
  onSelect,
  currentType = 'one-to-many',
}: {
  onSelect: (type: RelationshipType) => void;
  currentType?: RelationshipType;
}) => {
  return (
    <div className="bg-schemafy-bg border border-schemafy-light-gray rounded-lg p-3 min-w-32">
      <div className="text-xs font-medium text-schemafy-text mb-2">
        Relationship Type
      </div>
      {Object.entries(RELATIONSHIP_TYPES).map(([key, config]) => (
        <button
          key={key}
          onClick={() => onSelect(key as RelationshipType)}
          className={`
            w-full text-left px-2 py-1 rounded text-sm hover:bg-schemafy-secondary flex items-center gap-2
            ${currentType === key ? 'bg-schemafy-secondary border border-schemafy-light-gray' : ''}
          `}
        >
          <div
            className="w-6 h-0.5 rounded"
            style={{
              backgroundColor: config.style.stroke,
              borderStyle:
                'strokeDasharray' in config.style ? 'dashed' : 'solid',
            }}
          />
          {config.label}
        </button>
      ))}
    </div>
  );
};
