import * as React from 'react';
import * as AvatarPrimitive from '@radix-ui/react-avatar';
import { type VariantProps, cva } from 'class-variance-authority';
import { cn } from '@/lib';

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
  extends React.ComponentPropsWithoutRef<typeof AvatarPrimitive.Root>,
    VariantProps<typeof avatarVariants> {
  src?: string;
  alt?: string;
}

export const Avatar = ((
  { className, size, deactivate, src, alt, ...props }: CustomAvatarProps,
  ref: React.Ref<HTMLDivElement>,
) => {
  return (
    <AvatarRoot ref={ref} className={cn(avatarVariants({ size, deactivate }), className)} {...props}>
      {src && <AvatarImage src={src} alt={alt || 'Avatar'} />}
    </AvatarRoot>
  );
}) as React.ComponentType<CustomAvatarProps>;

const AvatarRoot = ({ className, ref, ...props }: React.ComponentProps<typeof AvatarPrimitive.Root>) => (
  <AvatarPrimitive.Root
    data-slot="avatar"
    ref={ref}
    className={cn('relative flex shrink-0 overflow-hidden', className)}
    {...props}
  />
);

const AvatarImage = ({ className, ref, ...props }: React.ComponentProps<typeof AvatarPrimitive.Image>) => (
  <AvatarPrimitive.Image ref={ref} className={cn('aspect-square h-full w-full', className)} {...props} />
);
