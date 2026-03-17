import { useEffect, useRef, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useReactFlow } from '@xyflow/react';
import { collaborationStore } from '@/store/collaboration.store';
import { authStore } from '@/store/auth.store';
import { CURSOR_FADE_DURATION_MS, CURSOR_IDLE_MS, getCursorColor } from '../utils';
import { CursorLabel, CursorPointer } from '.';

interface RemoteCursorProps {
  userId: string;
}

const RemoteCursor = observer(({userId}: RemoteCursorProps) => {
  const cursor = collaborationStore.cursors.get(userId);
  const {flowToScreenPosition} = useReactFlow();
  const [isIdle, setIsIdle] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const color = getCursorColor(userId);

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

  return (
    <div
      className="fixed pointer-events-none z-40"
      style={{
        left: 0,
        top: 0,
        transform: `translate3d(${screenPos.x}px, ${screenPos.y}px, 0)`,
        opacity: isIdle ? 0 : 1,
        transition: `opacity ${CURSOR_FADE_DURATION_MS}ms ease`,
      }}
    >
      <CursorPointer color={color}/>
      <CursorLabel name={cursor.userName} color={color}/>
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
