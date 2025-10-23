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
  date: Date;
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
    <li className="flex flex-col gap-1 items-center px-3 py-2 w-full rounded-[10px] border border-schemafy-light-gray">
      <div className="flex w-full justify-between items-center gap-4">
        <p className="font-overline-sm text-schemafy-text flex gap-1 items-center">
          {name}
          <Edit size={12} onClick={handleEditClick} cursor="pointer" />
          <Trash size={12} color="var(--color-schemafy-destructive)" onClick={handleDeleteClick} cursor="pointer" />
        </p>
        <Tag count={count} isEntity={!!description} />
      </div>
      <div className="flex w-full justify-between items-center font-body-xs text-schemafy-dark-gray">
        <p>{description}</p>
        <p>Last Updated: {formatDate(date)}</p>
      </div>
    </li>
  );
};

const Tag = ({ count, isEntity }: { count: number; isEntity: boolean }) => {
  return (
    <div className="px-2 py-0.5 flex bg-schemafy-light-gray font-body-xs text-schemafy-dark-gray rounded-full">
      {count} {isEntity ? 'fields' : 'entities'}
    </div>
  );
};
