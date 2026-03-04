import type { ViewModeColumnProps, ColumnBadgesProps } from '../../types';
import { formatTypeDisplay } from './utils';

export const ViewModeColumn = ({ column }: ViewModeColumnProps) => {
  const nameClassName = column.isPrimaryKey
    ? 'font-bold text-schemafy-yellow'
    : column.isForeignKey
      ? 'font-bold text-schemafy-green'
      : 'text-schemafy-text';

  return (
    <div className="p-2 text-schemafy-text">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <span className={`text-sm ${nameClassName}`}>{column.name}</span>
          <span className="text-xs text-schemafy-dark-gray font-mono">
            ({formatTypeDisplay(column.type, column.lengthScale)})
          </span>
        </div>

        <ColumnBadges column={column} />
      </div>
    </div>
  );
};

const ColumnBadges = ({ column }: ColumnBadgesProps) => {
  return (
    <div className="flex items-center gap-1">
      {column.isPrimaryKey && (
        <span className="text-xs text-schemafy-yellow font-medium">PK</span>
      )}
      {column.isForeignKey && (
        <span className="text-xs text-schemafy-green font-medium">FK</span>
      )}
      {!column.isPrimaryKey && column.isNotNull && (
        <span className="text-xs text-schemafy-destructive font-medium">*</span>
      )}
      {!column.isPrimaryKey && column.isUnique && (
        <span className="text-xs text-schemafy-blue font-medium">UQ</span>
      )}
    </div>
  );
};
