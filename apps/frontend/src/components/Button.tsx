import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { type VariantProps, cva } from 'class-variance-authority';
import { cn } from '@/lib';
import { Link } from 'react-router-dom';

const buttonVariants = cva(
  `inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg transition-all duration-200 
  disabled:pointer-events-none disabled:opacity-50 active:scale-95 font-heading-xs`,
  {
    variants: {
      variant: {
        default: 'bg-schemafy-button-bg text-schemafy-button-text',
        secondary: 'bg-schemafy-secondary text-schemafy-text',
        destructive: 'bg-schemafy-destructive text-white',
        outline: 'border border-schemafy-text text-schemafy-text',
        none: 'text-schemafy-text font-overline-sm',
      },
      size: {
        dropdown: 'py-2 font-overline-xs',
        default: 'px-4 h-10',
        none: '',
      },
      fullWidth: {
        true: 'w-full',
      },
      round: {
        true: 'rounded-full',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
);

interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  children?: ReactNode;
  to?: string;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'default',
      size = 'default',
      children,
      disabled = false,
      fullWidth = false,
      round = false,
      to,
      ...props
    },
    ref,
  ) => {
    if (to) {
      return (
        <Link to={to}>
          <button
            className={cn(
              buttonVariants({ variant, size, fullWidth, round, className }),
            )}
            disabled={disabled}
            ref={ref}
            {...props}
          >
            {children}
          </button>
        </Link>
      );
    }

    return (
      <button
        className={cn(
          buttonVariants({ variant, size, fullWidth, round, className }),
        )}
        disabled={disabled}
        ref={ref}
        {...props}
      >
        {children}
      </button>
    );
  },
);
