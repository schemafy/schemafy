export type PostCursor = {
  type: 'CURSOR';
  cursor: {
    x: number;
    y: number;
  };
};

export type PostSchemaFocus = {
  type: 'SCHEMA_FOCUS';
  schemaId: string;
};

export type PostChat = {
  type: 'CHAT';
  content: string;
};

export type ReceiveJoin = {
  type: 'JOIN';
  userId: string;
  userName: string;
  sessionId: string;
  timestamp: string;
};

export type ReceiveLeave = {
  type: 'LEAVE';
  userId: string;
  userName: string;
  sessionId: string;
  timestamp: string;
};

export type ReceiveCursor = {
  type: 'CURSOR';
  sessionId: string;
  userInfo: {
    userId: string;
    userName: string;
  };
  cursor: {
    x: number;
    y: number;
  };
  timestamp: string;
};

export type ReceiveSchemaFocus = {
  type: 'SCHEMA_FOCUS';
  userId: string;
  userName: string;
  sessionId: string;
  schemaId: string;
  timestamp: string;
};

export type ReceiveChat = {
  type: 'CHAT';
  messageId: string;
  userId: string;
  userName: string;
  sessionId: string;
  content: string;
  timestamp: string;
  position?: { x: number; y: number };
};

export type ReceiveErdMutated = {
  type: 'ERD_MUTATED';
  schemaId: string;
  affectedTableIds: string[];
  timestamp: number;
};

export type ReceiveSessionReady = {
  type: 'SESSION_READY';
  sessionId: string;
  timestamp: number;
};

export type WebSocketMessage =
  | ReceiveSessionReady
  | ReceiveJoin
  | ReceiveLeave
  | ReceiveCursor
  | ReceiveSchemaFocus
  | ReceiveChat
  | ReceiveErdMutated;

export type ChatMessage = {
  messageId: string;
  userId: string;
  userName: string;
  sessionId: string;
  content: string;
  timestamp: string;
  position?: { x: number; y: number };
};

export type CursorPosition = {
  userId: string;
  userName: string;
  sessionId: string;
  x: number;
  y: number;
};
