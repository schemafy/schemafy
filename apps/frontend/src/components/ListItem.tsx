export const ListItem = ({
  name,
  count,
  description,
  date,
}: {
  name: string;
  count: number;
  description?: string;
  date: string;
}) => {
  return (
    <li className="flex flex-col gap-1 items-center px-3 py-2 w-full rounded-[10px] border border-schemafy-light-gray">
      <div className="flex w-full justify-between items-center">
        <p className="font-overline-sm text-schemafy-text">{name}</p>
        <Tag count={count} isEntity={!!description} />
      </div>
      <div className="flex w-full justify-between items-center font-body-xs text-schemafy-dark-gray">
        <p>{description}</p>
        <p>Last Updated: {date}</p>
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
