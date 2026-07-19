import { observer } from 'mobx-react-lite';
import { collaborationStore } from '@/store/collaboration.store';
import type { ConnectionStatus } from '@/store/collaboration.store';
import { cn } from '@/lib';

const STATUS_CONFIG: Record<
  ConnectionStatus,
  { label: string; dotColor: string; bgColor: string }
> = {
  connected: {
    label: 'Connected',
    dotColor: 'bg-schemafy-green',
    bgColor: 'bg-schemafy-green/10',
  },
  connecting: {
    label: 'Connecting...',
    dotColor: 'bg-yellow-400',
    bgColor: 'bg-yellow-400/10',
  },
  reconnecting: {
    label: 'Reconnecting...',
    dotColor: 'bg-orange-400',
    bgColor: 'bg-orange-400/10',
  },
  disconnected: {
    label: 'Disconnected',
    dotColor: 'bg-schemafy-destructive',
    bgColor: 'bg-schemafy-destructive/10',
  },
};

export const ConnectionStatusIndicator = observer(() => {
  const status = collaborationStore.connectionStatus;
  const config = STATUS_CONFIG[status];

  if (status === 'connected') return null;

  return (
    <div
      className={cn(
        'pointer-events-auto flex items-center gap-2 rounded-xl px-3 py-1.5 font-caption-sm shadow-sm backdrop-blur-sm',
        config.bgColor,
      )}
      role="status"
      aria-live="polite"
    >
      <span
        className={cn(
          'h-2 w-2 shrink-0 rounded-full',
          status === 'connecting' || status === 'reconnecting'
            ? 'animate-pulse'
            : '',
          config.dotColor,
        )}
      />
      <span className="text-schemafy-text">{config.label}</span>
    </div>
  );
});
