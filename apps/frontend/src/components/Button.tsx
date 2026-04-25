import {
  type AnchorHTMLAttributes,
  type ButtonHTMLAttributes,
  forwardRef,
  type MouseEventHandler,
  type ReactNode,
} from 'react';
import { type VariantProps, cva } from 'class-variance-authority';
import { cn } from '@/lib';
import { Link, type LinkProps } from '@tanstack/react-router';

const buttonVariants = cva(
  `inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg transition-all duration-200 
  disabled:pointer-events-none disabled:opacity-50 active:scale-95 font-heading-xs focus:outline-none`,
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
        dropdown: 'py-2 font-overline-xs min-w-[5rem]',
        default: 'px-4 h-10',
        sm: 'py-0.3 px-3 font-overline-xs min-w-[4rem]',
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

type ButtonVariantProps = VariantProps<typeof buttonVariants>;

type ButtonBaseProps = ButtonVariantProps & {
  children?: ReactNode;
  className?: string;
  disabled?: boolean;
};

type ButtonProps = ButtonBaseProps &
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, keyof ButtonBaseProps>;

type ButtonLinkProps = ButtonBaseProps &
  Omit<
    AnchorHTMLAttributes<HTMLAnchorElement>,
    keyof ButtonBaseProps | 'href'
  > &
  Pick<LinkProps, 'hash' | 'params' | 'search' | 'to'>;

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
      ...props
    }: ButtonProps,
    ref,
  ) => {
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

Button.displayName = 'Button';

export const ButtonLink = forwardRef<HTMLAnchorElement, ButtonLinkProps>(
  (
    {
      className,
      variant = 'default',
      size = 'default',
      children,
      disabled = false,
      fullWidth = false,
      round = false,
      onClick,
      to,
      ...props
    },
    ref,
  ) => {
    const handleClick: MouseEventHandler<HTMLAnchorElement> = (event) => {
      if (disabled) {
        event.preventDefault();
        return;
      }

      onClick?.(event);
    };

    return (
      <Link
        aria-disabled={disabled}
        className={cn(
          buttonVariants({ variant, size, fullWidth, round, className }),
          disabled && 'pointer-events-none opacity-50',
        )}
        onClick={handleClick}
        ref={ref}
        to={to}
        {...props}
      >
        {children}
      </Link>
    );
  },
);

ButtonLink.displayName = 'ButtonLink';
