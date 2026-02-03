import { makeAutoObservable, runInAction } from 'mobx';
import type {
  ChatMessage,
  CursorPosition,
  PostChat,
  PostCursor,
  RecieveChat,
  RecieveCursor,
  RecieveLeave,
  WebSocketMessage,
} from '@/lib/api/collaboration/types';
import { authStore } from './auth.store';

class CollaborationStore {
  private ws: WebSocket | null = null;

  cursors: Map<string, CursorPosition> = new Map();
  projectId: string | null = null;
  private chatMessageListeners: Set<(message: ChatMessage) => void> = new Set();

  constructor() {
    makeAutoObservable(this);
  }

  private reconnectTimeoutId: number | null = null;

  get currentUser() {
    return authStore.user;
  }

  private setupWebSocketListeners() {
    if (!this.ws) return;

    this.ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data) as WebSocketMessage;
        this.handleMessage(message);
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    this.ws.onclose = () => {
      if (!this.projectId || this.reconnectTimeoutId) return;

      this.reconnectTimeoutId = window.setTimeout(() => {
        if (!this.projectId) return;

        this.reconnectTimeoutId = null;
        this.connect(this.projectId);
      }, 3000);
    };
  }

  connect(projectId: string) {
    if (
      this.ws?.readyState === WebSocket.OPEN ||
      this.ws?.readyState === WebSocket.CONNECTING
    ) {
      if (this.projectId === projectId) {
        return;
      }
      this.disconnect();
    }

    this.projectId = projectId;
    const baseUrl =
      import.meta.env.VITE_BFF_WS_URL || 'ws://localhost:4000/ws/collaboration';
    const wsUrl = `${baseUrl}?projectId=${projectId}`;

    try {
      this.ws = new WebSocket(wsUrl);
      this.setupWebSocketListeners();
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
    }
  }

  disconnect() {
    if (this.ws) {
      if (this.reconnectTimeoutId) {
        clearTimeout(this.reconnectTimeoutId);
        this.reconnectTimeoutId = null;
      }
      this.ws.close();
      this.ws = null;
    }

    runInAction(() => {
      this.projectId = null;
      this.cursors.clear();
    });
  }

  onChatMessage(listener: (message: ChatMessage) => void) {
    this.chatMessageListeners.add(listener);
    return () => {
      this.chatMessageListeners.delete(listener);
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
          if (this.ws?.readyState !== WebSocket.OPEN) return;
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
      cursor: { x, y },
    };

    this.send(message, (error) => {
      console.error('Failed to send cursor:', error);
    });
  }

  private send(
    message: PostChat | PostCursor,
    onError?: (error: unknown) => void,
  ) {
    if (this.ws?.readyState !== WebSocket.OPEN) {
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
    }
  }

  private handleChatMessage(message: RecieveChat) {
    const chatMessage: ChatMessage = {
      messageId: message.messageId,
      userId: message.userId,
      userName: message.userName,
      content: message.content,
      timestamp: message.timestamp,
    };

    this.chatMessageListeners.forEach((listener) => listener(chatMessage));
  }

  private handleCursorMessage(message: RecieveCursor) {
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

  private handleLeaveMessage(message: RecieveLeave) {
    runInAction(() => {
      this.cursors.delete(message.userId);
    });
  }
}

export const collaborationStore = new CollaborationStore();
