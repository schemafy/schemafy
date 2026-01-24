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
import type { WorkerMessage, WorkerResponse } from '@/worker/types';
import { AuthStore } from './auth.store';

export class CollaborationStore {
  private static instance: CollaborationStore;
  private worker: SharedWorker | null = null;

  cursors: Map<string, CursorPosition> = new Map();
  projectId: string | null = null;
  private chatMessageListeners: Set<(message: ChatMessage) => void> = new Set();

  private constructor() {
    makeAutoObservable(this);
  }

  private reconnectTimeoutId: number | null = null;

  static getInstance(): CollaborationStore {
    if (!CollaborationStore.instance) {
      CollaborationStore.instance = new CollaborationStore();
    }
    return CollaborationStore.instance;
  }

  get currentUser() {
    return AuthStore.getInstance().user;
  }

  private setupWorkerListeners() {
    if (!this.worker) return;

    this.worker.port.onmessage = (event: MessageEvent<WorkerResponse>) => {
      const { type } = event.data;

      if (type === 'WS_MESSAGE') {
        const message = event.data.payload;
        this.handleMessage(message);
      } else if (type === 'WS_OPEN') {
        console.log('WebSocket connected via SharedWorker');
      } else if (type === 'WS_CLOSE') {
        if (!this.projectId || this.reconnectTimeoutId) return;

        this.reconnectTimeoutId = window.setTimeout(() => {
          if (!this.projectId) return;

          this.reconnectTimeoutId = null;
          this.connect(this.projectId);
        }, 3000);
      } else if (type === 'WS_ERROR') {
        console.error('WebSocket error from SharedWorker:', event.data.error);
      }
    };

    this.worker.port.start();
  }

  connect(projectId: string) {
    if (this.projectId === projectId && this.worker) {
      return;
    }

    if (this.projectId && this.projectId !== projectId) {
      this.disconnect();
    }

    this.projectId = projectId;

    try {
      if (!this.worker) {
        const userId = this.currentUser?.id ?? 'anonymous';

        const CollaborationWorker = new SharedWorker(
          new URL('../worker/collaboration.worker.ts', import.meta.url),
          { type: 'module', name: `collaboration-worker-${userId}` },
        );

        this.worker = CollaborationWorker;
        this.setupWorkerListeners();
      }

      const accessToken = AuthStore.getInstance().accessToken ?? '';

      this.worker.port.postMessage({
        type: 'CONNECT',
        projectId,
        token: accessToken,
      } as WorkerMessage);
    } catch (error) {
      console.error('Failed to initialize SharedWorker:', error);
    }
  }

  disconnect() {
    if (this.worker && this.projectId) {
      if (this.reconnectTimeoutId) {
        clearTimeout(this.reconnectTimeoutId);
        this.reconnectTimeoutId = null;
      }
      this.worker.port.postMessage({
        type: 'DISCONNECT',
        projectId: this.projectId,
      } as WorkerMessage);
      this.worker = null;
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
          if (!this.worker) return;
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
    if (!this.worker || !this.projectId) {
      console.error('SharedWorker is not ready');
      return;
    }

    try {
      this.worker.port.postMessage({
        type: 'SEND_MESSAGE',
        projectId: this.projectId,
        payload: message,
      } as WorkerMessage);
    } catch (error) {
      if (onError) {
        onError(error);
      } else {
        console.error('Failed to send message via SharedWorker:', error);
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
