import { cn } from '@/lib';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export const Pagination = ({
  currentPage,
  totalPages,
  onPageChange,
  className,
}: PaginationProps) => {
  return (
    <div className={cn('flex items-center gap-0.5', className)}>
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
        className="flex items-center justify-center min-w-[40px] h-[40px] rounded-full hover:bg-schemafy-secondary disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        aria-label="Previous page"
      >
        <svg width="18" height="18" viewBox="0 0 7 14" fill="none">
          <path
            fillRule="evenodd"
            clipRule="evenodd"
            d="M6.64797 12.227C6.86776 12.4468 6.86776 12.8032 6.64797 13.023C6.42818 13.2428 6.07182 13.2428 5.85203 13.023L0.227031 7.39797C0.121407 7.29246 0.0620575 7.14929 0.0620575 7C0.0620575 6.85071 0.121407 6.70754 0.227031 6.60203L5.85203 0.977031C6.07182 0.757239 6.42818 0.757239 6.64797 0.977031C6.86776 1.19682 6.86776 1.55318 6.64797 1.77297L1.42023 7L6.64797 12.227Z"
            fill="currentColor"
          />
        </svg>
      </button>

      {getPageNumbers(currentPage, totalPages).map((page, index) => (
        <button
          key={`${page}-${index}`}
          onClick={() => typeof page === 'number' && onPageChange(page)}
          disabled={page === '...'}
          className={cn(
            'min-w-[40px] h-[40px] rounded-full transition-colors text-schemafy-text',
            page === currentPage
              ? 'bg-schemafy-light-gray font-heading-xs'
              : 'hover:bg-schemafy-secondary font-body-sm',
            page === '...' && 'cursor-default hover:bg-transparent',
          )}
        >
          {page}
        </button>
      ))}

      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
        className="flex items-center justify-center min-w-[40px] h-[40px] rounded-full hover:bg-schemafy-secondary disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        aria-label="Previous page"
      >
        <svg width="18" height="18" viewBox="0 0 7 14" fill="none">
          <path
            fillRule="evenodd"
            clipRule="evenodd"
            d="M6.77297 7.39797L1.14797 13.023C0.928177 13.2428 0.571823 13.2428 0.352031 13.023C0.132239 12.8032 0.132239 12.4468 0.352031 12.227L5.57977 7L0.352031 1.77297C0.132239 1.55318 0.132239 1.19682 0.352031 0.977031C0.571823 0.757239 0.928177 0.757239 1.14797 0.977031L6.77297 6.60203C6.87859 6.70754 6.93794 6.85071 6.93794 7C6.93794 7.14929 6.87859 7.29246 6.77297 7.39797Z"
            fill="currentColor"
          />
        </svg>
      </button>
    </div>
  );
};

function getPageNumbers(
  currentPage: number,
  totalPages: number,
): (number | string)[] {
  const pages: (number | string)[] = [];

  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
  } else {
    if (currentPage <= 3) {
      for (let i = 1; i <= 3; i++) {
        pages.push(i);
      }
      pages.push('...');
      pages.push(totalPages);
    } else if (currentPage >= totalPages - 2) {
      pages.push(1);
      pages.push('...');
      for (let i = totalPages - 2; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);
      pages.push('...');
      pages.push(currentPage - 1);
      pages.push(currentPage);
      pages.push(currentPage + 1);
      pages.push('...');
      pages.push(totalPages);
    }
  }

  return pages;
}
