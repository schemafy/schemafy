import { makeAutoObservable, runInAction } from 'mobx';
import type {
  ChatMessage,
  CursorPosition,
  PostChat,
  PostCursor,
  ReceiveChat,
  ReceiveCursor,
  ReceiveErdMutated,
  ReceiveLeave,
  WebSocketMessage,
} from '@/features/collaboration/api';
import { authStore } from './auth.store';
import { apiClient } from '@/lib/api/client';

const WEBSOCKET_URL =
  import.meta.env.VITE_WS_URL || 'ws://localhost:4000/ws/collaboration';

export class CollaborationStore {
  cursors: Map<string, CursorPosition> = new Map();
  projectId: string | null = null;
  sessionId: string | null = null;
  private ws: WebSocket | null = null;
  private reconnectTimeoutId: number | null = null;
  private chatMessageListeners: Set<(message: ChatMessage) => void> = new Set();
  private erdMutatedListeners: Set<(message: ReceiveErdMutated) => void> =
    new Set();

  constructor() {
    makeAutoObservable(this);
  }

  get currentUser() {
    return authStore.user;
  }

  connect(projectId: string, isReconnect = false) {
    if (!isReconnect && this.projectId === projectId && this.ws) return;

    if (this.projectId && this.projectId !== projectId) {
      this.disconnect();
    }

    this.projectId = projectId;

    const wsUrl = `${WEBSOCKET_URL}?projectId=${projectId}`;
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('WebSocket connected');
    };

    this.ws.onmessage = (event) => {
      try {
        const payload: WebSocketMessage = JSON.parse(event.data);

        if (payload.type === 'SESSION_READY') {
          runInAction(() => {
            this.sessionId = payload.sessionId;
          });
          apiClient.defaults.headers.common['X-Session-Id'] = payload.sessionId;
          return;
        }

        this.handleMessage(payload);
      } catch (error) {
        console.error('[WebSocket] Parse error', error);
      }
    };

    this.ws.onclose = () => {
      if (!this.projectId || this.reconnectTimeoutId) return;

      this.reconnectTimeoutId = window.setTimeout(() => {
        if (!this.projectId) return;

        this.reconnectTimeoutId = null;
        this.connect(this.projectId, true);
      }, 3000);
    };

    this.ws.onerror = (event) => {
      console.error('[WebSocket] error:', event);
    };
  }

  disconnect() {
    if (this.reconnectTimeoutId) {
      clearTimeout(this.reconnectTimeoutId);
      this.reconnectTimeoutId = null;
    }

    if (this.ws) {
      this.ws.onclose = null;
      this.ws.close();
      this.ws = null;
    }

    delete apiClient.defaults.headers.common['X-Session-Id'];
    runInAction(() => {
      this.projectId = null;
      this.cursors.clear();
      this.sessionId = null;
    });
  }

  onChatMessage(listener: (message: ChatMessage) => void) {
    this.chatMessageListeners.add(listener);

    return () => {
      this.chatMessageListeners.delete(listener);
    };
  }

  onErdMutated(listener: (message: ReceiveErdMutated) => void) {
    this.erdMutatedListeners.add(listener);

    return () => {
      this.erdMutatedListeners.delete(listener);
    };
  }

  sendMessage(content: string) {
    const message: PostChat = {
      type: 'CHAT',
      content,
    };

    this.send(message, (error) => {
      console.error('Failed to send chat message:', error);
      setTimeout(() => {
        try {
          this.send(message);
        } catch (retryError) {
          console.error('Retry failed:', retryError);
        }
      }, 500);
    });
  }

  sendCursor(x: number, y: number) {
    const user = this.currentUser;

    if (!user) {
      console.error('User is not logged in');
      return;
    }

    runInAction(() => {
      this.cursors.set(user.id, {
        userId: user.id,
        userName: user.name,
        x,
        y,
      });
    });

    const message: PostCursor = {
      type: 'CURSOR',
      cursor: {x, y},
    };

    this.send(message, (error) => {
      console.error('Failed to send cursor:', error);
    });
  }

  private send(
    message: PostChat | PostCursor,
    onError?: (error: unknown) => void,
  ) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('WebSocket is not ready');
      return;
    }

    try {
      this.ws.send(JSON.stringify(message));
    } catch (error) {
      if (onError) {
        onError(error);
      } else {
        console.error('Failed to send message:', error);
      }
    }
  }

  private handleMessage(message: WebSocketMessage) {
    switch (message.type) {
      case 'CHAT':
        this.handleChatMessage(message);
        break;
      case 'CURSOR':
        this.handleCursorMessage(message);
        break;
      case 'JOIN':
        break;
      case 'LEAVE':
        this.handleLeaveMessage(message);
        break;
      case 'SCHEMA_FOCUS':
        break;
      case 'ERD_MUTATED':
        this.handleErdMutatedMessage(message);
        break;
    }
  }

  private handleErdMutatedMessage(message: ReceiveErdMutated) {
    this.erdMutatedListeners.forEach((listener) => listener(message));
  }

  private handleChatMessage(message: ReceiveChat) {
    const chatMessage: ChatMessage = {
      messageId: message.messageId,
      userId: message.userId,
      userName: message.userName,
      content: message.content,
      timestamp: message.timestamp,
      position: message.position,
    };

    this.chatMessageListeners.forEach((listener) => listener(chatMessage));
  }

  private handleCursorMessage(message: ReceiveCursor) {
    const cursorPosition: CursorPosition = {
      userId: message.userInfo.userId,
      userName: message.userInfo.userName,
      x: message.cursor.x,
      y: message.cursor.y,
    };

    runInAction(() => {
      this.cursors.set(cursorPosition.userId, cursorPosition);
    });
  }

  private handleLeaveMessage(message: ReceiveLeave) {
    runInAction(() => {
      this.cursors.delete(message.userId);
    });
  }
}

export const collaborationStore = new CollaborationStore();
