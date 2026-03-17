import { useEffect, useRef, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useReactFlow } from '@xyflow/react';
import { collaborationStore } from '@/store/collaboration.store.ts';
import { authStore } from '@/store/auth.store.ts';

const CURSOR_IDLE_MS = 5000;

const CURSOR_COLORS = [
  '#FF6B6B',
  '#4ECDC4',
  '#45B7D1',
  '#96CEB4',
  '#FECA57',
  '#FF9FF3',
  '#54A0FF',
  '#FF9F43',
];

const getUserColor = (userId: string) => {
  let hash = 0;
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash);
  }
  return CURSOR_COLORS[Math.abs(hash) % CURSOR_COLORS.length];
};

interface RemoteCursorProps {
  userId: string;
}

const RemoteCursor = observer(({userId}: RemoteCursorProps) => {
  const cursor = collaborationStore.cursors.get(userId);
  const {flowToScreenPosition} = useReactFlow();
  const [isIdle, setIsIdle] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setIsIdle(false);

    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    timerRef.current = setTimeout(() => {
      setIsIdle(true);
    }, CURSOR_IDLE_MS);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [cursor?.x, cursor?.y]);

  if (!cursor) return null;

  const screenPos = flowToScreenPosition({x: cursor.x, y: cursor.y});
  const color = getUserColor(userId);

  return (
    <div
      className="fixed pointer-events-none z-40 transition-opacity duration-500"
      style={{
        left: 0,
        top: 0,
        transform: `translate3d(${screenPos.x}px, ${screenPos.y}px, 0)`,
        opacity: isIdle ? 0 : 1,
      }}
    >
      <svg
        width="16"
        height="22"
        viewBox="0 0 16 22"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M0 0 L0 18 L4.5 14 L7 20 L9.5 19 L7 13 L13 13 Z"
          fill={color}
          stroke="white"
          strokeWidth="1.5"
          strokeLinejoin="round"
        />
      </svg>
      <div
        className="mt-0.5 px-1.5 py-0.5 rounded text-white whitespace-nowrap font-overline-xs"
        style={{backgroundColor: color}}
      >
        {cursor.userName}
      </div>
    </div>
  );
});

export const RemoteCursors = observer(() => {
  const currentUserId = authStore.user?.id;
  const cursorIds = Array.from(collaborationStore.cursors.keys()).filter(
    (id) => id !== currentUserId,
  );

  return (
    <>
      {cursorIds.map((userId) => (
        <RemoteCursor key={userId} userId={userId}/>
      ))}
    </>
  );
});
