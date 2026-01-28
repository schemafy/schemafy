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

export type RecieveJoin = {
  type: 'JOIN';
  userId: string;
  userName: string;
  timestamp: string;
};

export type RecieveLeave = {
  type: 'LEAVE';
  userId: string;
  userName: string;
  timestamp: string;
};

export type RecieveCursor = {
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

export type RecieveSchemaFocus = {
  type: 'SCHEMA_FOCUS';
  userId: string;
  userName: string;
  schemaId: string;
  timestamp: string;
};

export type RecieveChat = {
  type: 'CHAT';
  messageId: string;
  userId: string;
  userName: string;
  content: string;
  timestamp: string;
  position?: { x: number; y: number };
};

export type WebSocketMessage =
  | RecieveJoin
  | RecieveLeave
  | RecieveCursor
  | RecieveSchemaFocus
  | RecieveChat;

export type ChatMessage = {
  messageId: string;
  userId: string;
  userName: string;
  content: string;
  timestamp: string;
  position?: { x: number; y: number };
};

export type CursorPosition = {
  userId: string;
  userName: string;
  x: number;
  y: number;
};
