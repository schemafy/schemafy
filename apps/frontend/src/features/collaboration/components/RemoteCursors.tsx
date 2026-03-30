import { observer } from 'mobx-react-lite';
import { useReactFlow } from '@xyflow/react';
import { collaborationStore } from '@/store/collaboration.store';
import { useChatMessages } from '@/hooks';
import { CURSOR_TRANSITION_MS, getCursorColor } from '../utils';
import { CursorLabel } from './CursorLabel';
import { CursorPointer } from './CursorPointer';
import { ChatBubble } from './ChatBubble';

interface RemoteCursorProps {
  sessionId: string;
}

const RemoteCursor = observer(({sessionId}: RemoteCursorProps) => {
  const isMe = sessionId === collaborationStore.sessionId;
  const cursor = collaborationStore.cursors.get(sessionId);
  const activeMessage = collaborationStore.activeChatMessages.get(sessionId);
  const {flowToScreenPosition} = useReactFlow();
  const color = getCursorColor(sessionId);

  if (!cursor) return null;
  if (isMe && !activeMessage) return null;

  const screenPos = flowToScreenPosition({x: cursor.x, y: cursor.y});

  return (
    <div
      className="fixed pointer-events-none z-40"
      style={{
        left: 0,
        top: 0,
        transform: `translate3d(${screenPos.x}px, ${screenPos.y}px, 0)`,
        transition: `transform ${CURSOR_TRANSITION_MS}ms linear`,
      }}
    >
      {!isMe && <CursorPointer color={color}/>}
      {activeMessage ? (
        <ChatBubble message={activeMessage} color={color}/>
      ) : (
        <CursorLabel name={cursor.userName} color={color}/>
      )}
    </div>
  );
});

export const RemoteCursors = observer(() => {
  useChatMessages();

  const cursorIds = Array.from(collaborationStore.cursors.keys());

  return (
    <>
      {cursorIds.map((sessionId) => (
        <RemoteCursor key={sessionId} sessionId={sessionId}/>
      ))}
    </>
  );
});