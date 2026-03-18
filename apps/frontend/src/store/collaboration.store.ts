import { action, computed, makeObservable, observable, runInAction, } from 'mobx';
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
import { toast } from "sonner";

const WEBSOCKET_URL =
  import.meta.env.VITE_WS_URL || 'ws://localhost:4000/ws/collaboration';

const MAX_RECONNECT_ATTEMPTS = 10;
const BASE_RECONNECT_DELAY_MS = 3000;
const MAX_RECONNECT_DELAY_MS = 60000;

export class CollaborationStore {
  cursors: Map<string, CursorPosition> = new Map();
  projectId: string | null = null;
  sessionId: string | null = null;
  private ws: WebSocket | null = null;
  private reconnectTimeoutId: number | null = null;
  private reconnectAttempts = 0;
  private chatMessageListeners: Set<(message: ChatMessage) => void> = new Set();
  private erdMutatedListeners: Set<(message: ReceiveErdMutated) => void> =
    new Set();

  constructor() {
    makeObservable(this, {
      cursors: observable,
      projectId: observable,
      sessionId: observable,
      currentUser: computed,
      connect: action,
      disconnect: action,
      sendMessage: action,
      sendCursor: action,
    });
  }

  get currentUser() {
    return authStore.user;
  }

  connect(projectId: string, isReconnect = false) {
    if (
      !isReconnect &&
      this.projectId === projectId &&
      this.ws &&
      (this.ws.readyState === WebSocket.OPEN ||
        this.ws.readyState === WebSocket.CONNECTING)
    )
      return;

    if (this.projectId && this.projectId !== projectId) {
      this.disconnect();
    }

    this.projectId = projectId;

    if (this.ws) {
      this.ws.onopen = null;
      this.ws.onmessage = null;
      this.ws.onclose = null;
      this.ws.onerror = null;
      this.ws.close();
      this.ws = null;
    }

    const wsUrl = `${WEBSOCKET_URL}?projectId=${projectId}`;
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
    };

    this.ws.onmessage = (event) => {
      try {
        const payload: WebSocketMessage = JSON.parse(event.data);
        console.log(this.sessionId);

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

      if (this.reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        toast.error("Network Error, please try again later.");
        return;
      }

      const delay = Math.min(
        BASE_RECONNECT_DELAY_MS * Math.pow(2, this.reconnectAttempts),
        MAX_RECONNECT_DELAY_MS,
      );
      this.reconnectAttempts++;

      this.reconnectTimeoutId = window.setTimeout(() => {
        if (!this.projectId) return;

        this.reconnectTimeoutId = null;
        this.connect(this.projectId, true);
      }, delay);
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
      this.ws.onopen = null;
      this.ws.onmessage = null;
      this.ws.onclose = null;
      this.ws.onerror = null;
      this.ws.close();
      this.ws = null;
    }

    this.reconnectAttempts = 0;
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
        this.send(message, (retryError) => {
          console.error('Retry failed:', retryError);
        });
      }, 500);
    });
  }

  sendCursor(x: number, y: number) {
    const user = this.currentUser;

    if (!user) {
      console.error('User is not logged in');
      return;
    }

    const sessionId = this.sessionId;

    if (!sessionId) {
      console.error('Session ID is not available');
      return;
    }

    runInAction(() => {
      const cursorPosition: CursorPosition = {
        sessionId,
        userId: user.id,
        userName: user.name,
        x,
        y,
      };

      this.cursors.set(sessionId, cursorPosition);
    });

    const message: PostCursor = {
      type: 'CURSOR',
      cursor: {x, y},
    };

    this.send(message);
  }

  private send(
    message: PostChat | PostCursor,
    onError?: (error: unknown) => void,
  ) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      if (onError) {
        onError(new Error('WebSocket is not ready'));
      }
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
      sessionId: message.sessionId,
      content: message.content,
      timestamp: message.timestamp,
      position: message.position,
    };

    this.chatMessageListeners.forEach((listener) => listener(chatMessage));
  }

  private handleCursorMessage(message: ReceiveCursor) {
    const cursorPosition: CursorPosition = {
      sessionId: message.sessionId,
      userId: message.userInfo.userId,
      userName: message.userInfo.userName,
      x: message.cursor.x,
      y: message.cursor.y,
    };

    runInAction(() => {
      this.cursors.set(cursorPosition.sessionId, cursorPosition);
    });
  }

  private handleLeaveMessage(message: ReceiveLeave) {
    runInAction(() => {
      this.cursors.delete(message.sessionId);
    });
  }
}

export const collaborationStore = new CollaborationStore();
