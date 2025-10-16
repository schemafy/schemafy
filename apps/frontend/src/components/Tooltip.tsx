import * as React from 'react';
import * as TooltipPrimitive from '@radix-ui/react-tooltip';

import { cn } from '@/lib/utils';

const TooltipProvider = TooltipPrimitive.Provider;

const Tooltip = TooltipPrimitive.Root;

const TooltipTrigger = TooltipPrimitive.Trigger;

type Direction = 'top' | 'bottom' | 'left' | 'right';

interface TooltipContentProps extends React.ComponentPropsWithoutRef<typeof TooltipPrimitive.Content> {
  ref?: React.Ref<React.ElementRef<typeof TooltipPrimitive.Content>>;
  direction?: Direction;
}

const TooltipContent = ({ className, sideOffset = 16, ref, direction = 'top', ...props }: TooltipContentProps) => {
  const arrowStyles = {
    top: 'before:absolute before:left-1/2 before:-translate-x-1/2 before:-bottom-1 before:border-l-[6px] before:border-l-transparent before:border-r-[6px] before:border-r-transparent before:border-t-[6px] before:border-t-schemafy-bg before:drop-shadow-md',
    bottom:
      'before:absolute before:left-1/2 before:-translate-x-1/2 before:-top-1 before:border-l-[6px] before:border-l-transparent before:border-r-[6px] before:border-r-transparent before:border-b-[6px] before:border-b-schemafy-bg before:drop-shadow-md',
    left: 'before:absolute before:top-1/2 before:-translate-y-1/2 before:-right-1 before:border-t-[6px] before:border-t-transparent before:border-b-[6px] before:border-b-transparent before:border-l-[6px] before:border-l-schemafy-bg before:drop-shadow-md',
    right:
      'before:absolute before:top-1/2 before:-translate-y-1/2 before:-left-1 before:border-t-[6px] before:border-t-transparent before:border-b-[6px] before:border-b-transparent before:border-r-[6px] before:border-r-schemafy-bg before:drop-shadow-md',
  };

  return (
    <TooltipPrimitive.Content
      ref={ref}
      sideOffset={sideOffset}
      side={direction}
      className={cn(
        'relative z-50 rounded-md bg-schemafy-bg px-3 py-1.5 font-caption-md text-schemafy-text shadow-md',
        'animate-in fade-in-0 zoom-in-95',
        'data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95',
        'data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2',
        arrowStyles[direction],
        className,
      )}
      {...props}
    />
  );
};

TooltipContent.displayName = 'TooltipContent';

export { Tooltip, TooltipTrigger, TooltipContent, TooltipProvider };
