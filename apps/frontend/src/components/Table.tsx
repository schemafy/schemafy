import { type ReactNode } from 'react';
import { cn } from '@/lib';

interface TableProps {
  children: ReactNode;
  className?: string;
}

interface TableHeaderProps {
  children: ReactNode;
  className?: string;
}

interface TableBodyProps {
  children: ReactNode;
  className?: string;
}

interface TableRowProps {
  children: ReactNode;
  className?: string;
}

interface TableHeadProps {
  children?: ReactNode;
  className?: string;
}

interface TableCellProps {
  children: ReactNode;
  className?: string;
}

export const Table = ({ children, className }: TableProps) => {
  return (
    <div className="w-full overflow-auto">
      <table className={cn('w-full border-collapse', className)}>
        {children}
      </table>
    </div>
  );
};

export const TableHeader = ({ children, className }: TableHeaderProps) => {
  return <thead className={cn('', className)}>{children}</thead>;
};

export const TableBody = ({ children, className }: TableBodyProps) => {
  return <tbody className={cn('', className)}>{children}</tbody>;
};

export const TableRow = ({ children, className }: TableRowProps) => {
  return (
    <tr
      className={cn(
        'border-b border-schemafy-light-gray hover:bg-schemafy-secondary/50 transition-colors',
        className,
      )}
    >
      {children}
    </tr>
  );
};

export const TableHead = ({ children, className }: TableHeadProps) => {
  return (
    <th
      className={cn(
        'text-left py-4 px-6 font-overline-md text-schemafy-dark-gray',
        className,
      )}
    >
      {children}
    </th>
  );
};

export const TableCell = ({ children, className }: TableCellProps) => {
  return (
    <td className={cn('py-4 px-6 font-body-md text-schemafy-text', className)}>
      {children}
    </td>
  );
};
