import { makeAutoObservable, runInAction } from 'mobx';
import type {
  ChatMessage,
  CursorPosition,
  RecieveChat,
  RecieveCursor,
  RecieveLeave,
  WebSocketMessage,
} from '@/lib/api/collaboration/types';
import { AuthStore } from './auth.store';

export class CollaborationStore {
  private static instance: CollaborationStore;
  private ws: WebSocket | null = null;

  isConnected = false;
  messages: ChatMessage[] = [];
  cursors: Map<string, CursorPosition> = new Map();
  projectId: string | null = null;

  private constructor() {
    makeAutoObservable(this);
  }

  static getInstance(): CollaborationStore {
    if (!CollaborationStore.instance) {
      CollaborationStore.instance = new CollaborationStore();
    }
    return CollaborationStore.instance;
  }

  connect(projectId: string) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      if (this.projectId === projectId) {
        return;
      }
      this.disconnect();
    }

    this.projectId = projectId;
    const wsUrl = `ws://localhost:4000/ws/collaboration?projectId=${projectId}`;

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        runInAction(() => {
          this.isConnected = true;
        });
      };

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
        runInAction(() => {
          this.isConnected = false;
        });
      };
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    runInAction(() => {
      this.isConnected = false;
      this.projectId = null;
      this.messages = [];
      this.cursors.clear();
    });
  }

  sendMessage(content: string) {
    if (!this.isConnected || !this.ws) {
      console.error('WebSocket is not connected');
      return;
    }

    const message = {
      type: 'CHAT',
      content,
    };

    this.ws.send(JSON.stringify(message));
  }

  sendCursor(x: number, y: number) {
    if (!this.isConnected || !this.ws) {
      return;
    }

    const authStore = AuthStore.getInstance();
    const userName = authStore.user?.name;

    if (userName) {
      runInAction(() => {
        this.cursors.set(userName, {
          userName,
          x,
          y,
        });
      });
    }

    const message = {
      type: 'CURSOR',
      cursor: { x, y },
    };

    this.ws.send(JSON.stringify(message));
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

    runInAction(() => {
      this.messages.push(chatMessage);
      if (this.messages.length > 50) {
        this.messages.shift();
      }
    });
  }

  private handleCursorMessage(message: RecieveCursor) {
    const cursorPosition: CursorPosition = {
      userName: message.cursor.userName,
      x: message.cursor.x,
      y: message.cursor.y,
    };

    runInAction(() => {
      this.cursors.set(cursorPosition.userName, cursorPosition);
    });
  }

  private handleLeaveMessage(message: RecieveLeave) {
    runInAction(() => {
      this.cursors.delete(message.userName);
    });
  }

  clearMessages() {
    runInAction(() => {
      this.messages = [];
    });
  }
}
