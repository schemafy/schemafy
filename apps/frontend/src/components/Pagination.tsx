import { cn } from '@/lib';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export const Pagination = ({
                             currentPage,
                             totalPages,
                             onPageChange,
                           }: PaginationProps) => {
  return (
    <div className="flex items-center justify-center gap-1 py-2">
      <button
        onClick={() => onPageChange(Math.max(1, currentPage - 1))}
        disabled={currentPage === 1}
        className="w-8 h-8 flex items-center justify-center rounded-full font-body-md text-schemafy-dark-gray disabled:opacity-30 hover:bg-schemafy-secondary transition-colors"
      >
        ‹
      </button>
      {Array.from({length: totalPages}, (_, i) => i + 1).map((page) => (
        <button
          key={page}
          onClick={() => onPageChange(page)}
          className={cn(
            'w-8 h-8 flex items-center justify-center rounded-full font-body-sm transition-colors',
            currentPage === page
              ? 'bg-schemafy-button-bg text-schemafy-button-text'
              : 'text-schemafy-dark-gray hover:bg-schemafy-secondary',
          )}
        >
          {page}
        </button>
      ))}
      <button
        onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
        disabled={currentPage >= totalPages || totalPages === 0}
        className="w-8 h-8 flex items-center justify-center rounded-full font-body-md text-schemafy-dark-gray disabled:opacity-30 hover:bg-schemafy-secondary transition-colors"
      >
        ›
      </button>
    </div>
  );
};