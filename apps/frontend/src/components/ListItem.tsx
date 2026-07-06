import { formatDate } from '@/lib/utils';
import { Edit, Trash } from 'lucide-react';

export const ListItem = ({
  name,
  count,
  description,
  date,
  onChange,
  onDelete,
}: {
  name: string;
  count: number;
  description?: string;
  date?: Date;
  onChange?: () => void;
  onDelete?: () => void;
}) => {
  const handleEditClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onChange?.();
  };

  const handleDeleteClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onDelete?.();
  };

  return (
    <li className="schemafy-subtle-card flex w-full flex-col items-center gap-2 px-4 py-3">
      <div className="flex w-full items-start justify-between gap-4">
        <div className="flex min-w-0 items-center gap-2">
          <p className="truncate font-overline-sm text-schemafy-text">{name}</p>
          <div className="flex shrink-0 gap-1">
            <button
              type="button"
              title="Edit"
              onClick={handleEditClick}
              className="schemafy-icon-button schemafy-focus-ring flex h-7 w-7 items-center justify-center"
            >
              <Edit size={12} />
            </button>
            <button
              type="button"
              title="Delete"
              onClick={handleDeleteClick}
              className="schemafy-icon-button schemafy-focus-ring flex h-7 w-7 items-center justify-center hover:text-schemafy-destructive"
            >
              <Trash size={12} />
            </button>
          </div>
        </div>
        <Tag count={count} isEntity={!!description} />
      </div>
      <div className="flex w-full flex-wrap items-center justify-between gap-x-4 gap-y-1 font-body-xs text-schemafy-dark-gray">
        <p className="min-w-0 break-words">{description}</p>
        {date && <p className="shrink-0">Last Updated: {formatDate(date)}</p>}
      </div>
    </li>
  );
};

const Tag = ({ count, isEntity }: { count: number; isEntity: boolean }) => {
  return (
    <div className="schemafy-badge flex px-2 py-0.5 font-body-xs">
      {count} {isEntity ? 'fields' : 'entities'}
    </div>
  );
};
