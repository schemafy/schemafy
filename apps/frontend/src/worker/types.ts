import type {
  CursorPosition,
  PostChat,
  PostCursor,
  PostSchemaFocus,
  WebSocketMessage,
} from '../lib/api/collaboration/types';

export type OutgoingMessage = PostCursor | PostSchemaFocus | PostChat;

export type UserInfo = {
  userId: string;
  userName: string;
};

export type WorkerMessage =
  | {
      type: 'CONNECT';
      projectId: string;
      token: string;
      userInfo: UserInfo;
    }
  | { type: 'DISCONNECT'; projectId: string }
  | { type: 'SEND_MESSAGE'; projectId: string; payload: OutgoingMessage };

export type WorkerResponse =
  | { type: 'WS_MESSAGE'; projectId: string; payload: WebSocketMessage }
  | { type: 'WS_OPEN'; projectId: string }
  | { type: 'WS_CLOSE'; projectId: string }
  | { type: 'WS_ERROR'; projectId: string; error: string }
  | {
      type: 'INITIAL_STATE';
      projectId: string;
      cursors: CursorPosition[];
      users: UserInfo[];
    };
