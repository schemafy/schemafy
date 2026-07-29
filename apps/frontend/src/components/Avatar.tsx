import * as React from 'react';
import * as AvatarPrimitive from '@radix-ui/react-avatar';
import { type VariantProps, cva } from 'class-variance-authority';
import { cn } from '@/lib';
import { getRandomColor, getInitials } from '@/features/collaboration/utils';

const avatarVariants = cva(`rounded-full`, {
  variants: {
    size: {
      default: 'w-10 h-10',
      dropdown: 'w-6 h-6',
    },
    deactivate: {
      true: 'after:absolute after:inset-0 after:bg-schemafy-bg after:opacity-70 after:rounded-full after:pointer-events-none',
    },
  },
  defaultVariants: {
    size: 'default',
  },
});

interface CustomAvatarProps
  extends
    React.ComponentPropsWithoutRef<typeof AvatarPrimitive.Root>,
    VariantProps<typeof avatarVariants> {
  src?: string;
  name?: string;
  alt?: string;
  color?: string;
}

export const Avatar = ((
  {
    className,
    size,
    deactivate,
    src,
    name,
    alt,
    color,
    ...props
  }: CustomAvatarProps,
  ref: React.Ref<HTMLDivElement>,
) => {
  return (
    <AvatarRoot
      ref={ref}
      className={cn(avatarVariants({ size, deactivate }), className)}
      {...props}
    >
      {src && <AvatarImage src={src} alt={alt || name || 'Avatar'} />}
      <AvatarPrimitive.Fallback
        className="flex h-full w-full items-center justify-center rounded-full text-xs font-medium"
        style={{ backgroundColor: color || getRandomColor(name || '') }}
      >
        {getInitials(name || '?')}
      </AvatarPrimitive.Fallback>
    </AvatarRoot>
  );
}) as React.ComponentType<CustomAvatarProps>;

const AvatarRoot = ({
  className,
  ref,
  ...props
}: React.ComponentProps<typeof AvatarPrimitive.Root>) => (
  <AvatarPrimitive.Root
    data-slot="avatar"
    ref={ref}
    className={cn(
      'relative flex shrink-0 overflow-hidden bg-schemafy-secondary ring-1 ring-schemafy-glass-border',
      className,
    )}
    {...props}
  />
);

const AvatarImage = ({
  className,
  ref,
  ...props
}: React.ComponentProps<typeof AvatarPrimitive.Image>) => (
  <AvatarPrimitive.Image
    ref={ref}
    className={cn('aspect-square h-full w-full', className)}
    {...props}
  />
);
