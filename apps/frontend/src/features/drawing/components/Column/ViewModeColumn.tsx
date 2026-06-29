import type { ViewModeColumnProps, ColumnBadgesProps } from '../../types';
import { formatTypeDisplay } from './utils';

export const ViewModeColumn = ({ column }: ViewModeColumnProps) => {
  const nameClassName = column.isPrimaryKey
    ? 'font-semibold text-schemafy-text'
    : column.isForeignKey
      ? 'font-semibold text-schemafy-text'
      : 'text-schemafy-text';

  return (
    <div className="px-3 py-2.5 text-schemafy-text">
      <div className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-2.5">
          <span className={`truncate text-sm ${nameClassName}`}>
            {column.name}
          </span>
          <span className="shrink-0 rounded-md bg-schemafy-secondary/60 px-1.5 py-0.5 font-mono text-[11px] text-schemafy-dark-gray">
            {formatTypeDisplay(column.type, column.typeArguments)}
          </span>
        </div>

        <ColumnBadges column={column} />
      </div>
    </div>
  );
};

const ColumnBadges = ({ column }: ColumnBadgesProps) => {
  return (
    <div className="flex shrink-0 items-center gap-1">
      {column.isPrimaryKey && (
        <span className="rounded-full border border-schemafy-yellow/20 bg-schemafy-yellow/10 px-1.5 py-0.5 text-[10px] font-semibold text-schemafy-yellow">
          PK
        </span>
      )}
      {column.isForeignKey && (
        <span className="rounded-full border border-schemafy-green/20 bg-schemafy-green/10 px-1.5 py-0.5 text-[10px] font-semibold text-schemafy-green">
          FK
        </span>
      )}
      {!column.isPrimaryKey && column.isNotNull && (
        <span className="rounded-full border border-schemafy-destructive/20 bg-schemafy-destructive/10 px-1.5 py-0.5 text-[10px] font-semibold text-schemafy-destructive">
          NN
        </span>
      )}
      {!column.isPrimaryKey && column.isUnique && (
        <span className="rounded-full border border-schemafy-blue/20 bg-schemafy-blue/10 px-1.5 py-0.5 text-[10px] font-semibold text-schemafy-blue">
          UQ
        </span>
      )}
    </div>
  );
};
