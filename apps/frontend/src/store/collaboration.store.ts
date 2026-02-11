import { makeAutoObservable, runInAction } from 'mobx';
import type {
  ChatMessage,
  CursorPosition,
  PostChat,
  PostCursor,
  ReceiveChat,
  ReceiveCursor,
  ReceiveLeave,
  WebSocketMessage,
} from '@/lib/api/collaboration/types';
import type { UserInfo, WorkerMessage, WorkerResponse } from '@/worker/types';
import { authStore } from './auth.store';

const SHARED_WORKER_ENABLE = typeof SharedWorker !== 'undefined';

export class CollaborationStore {
  private worker: SharedWorker | Worker | null = null;
  private port: MessagePort | Worker | null = null;

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

  private heartbeatIntervalId: number | null = null;

  private setupWorkerListeners() {
    if (!this.port) return;

    this.startHeartbeat();

    this.port.onmessage = (event: MessageEvent<WorkerResponse>) => {
      const { type } = event.data;

      if (type === 'WS_MESSAGE') {
        const message = event.data.payload;

        this.handleMessage(message);
      } else if (type === 'WS_OPEN') {
        console.log('WebSocket connected via Worker');
      } else if (type === 'WS_CLOSE') {
        if (!this.projectId || this.reconnectTimeoutId) return;

        this.reconnectTimeoutId = window.setTimeout(() => {
          if (!this.projectId) return;

          this.reconnectTimeoutId = null;
          this.connect(this.projectId, true);
        }, 3000);
      } else if (type === 'WS_ERROR') {
        console.error('WebSocket error from Worker:', event.data.error);
      } else if (type === 'INITIAL_STATE') {
        const { cursors } = event.data;

        runInAction(() => {
          cursors.forEach((cursor) => {
            this.cursors.set(cursor.userId, cursor);
          });
        });
      }
    };

    if (this.port instanceof MessagePort) {
      this.port.start();
    }
  }

  private startHeartbeat() {
    if (this.heartbeatIntervalId) {
      clearInterval(this.heartbeatIntervalId);
    }

    this.heartbeatIntervalId = window.setInterval(() => {
      if (this.port && this.projectId) {
        this.port.postMessage({
          type: 'PING',
          projectId: this.projectId,
        } as WorkerMessage);
      }
    }, 30000);
  }

  connect(projectId: string, isReconnect = false) {
    if (
      !isReconnect &&
      this.projectId === projectId &&
      this.worker &&
      this.port
    ) {
      return;
    }

    if (this.projectId && this.projectId !== projectId) {
      this.disconnect();
    }

    this.projectId = projectId;

    try {
      if (!this.worker) {
        const userId = this.currentUser?.id ?? 'anonymous';

        if (SHARED_WORKER_ENABLE) {
          const worker = new SharedWorker(
            new URL('../worker/collaboration.worker.ts', import.meta.url),
            { type: 'module', name: `collaboration-worker-${userId}` },
          );

          this.worker = worker;
          this.port = worker.port;
        } else {
          console.warn('SharedWorker not supported, falling back to Worker');

          const worker = new Worker(
            new URL('../worker/collaboration.worker.ts', import.meta.url),
            { type: 'module', name: `collaboration-worker-${userId}` },
          );

          this.worker = worker;
          this.port = worker;
        }

        this.setupWorkerListeners();
      }

      if (!this.currentUser) {
        console.error('user info is empty');
        return;
      }

      const userInfo: UserInfo = {
        userId: this.currentUser.id,
        userName: this.currentUser.name,
      };

      const message: WorkerMessage = {
        type: 'CONNECT',
        projectId,
        userInfo,
      };

      if (!this.port) {
        console.error('Worker port is not ready');
        return;
      }

      this.port.postMessage(message);
    } catch (error) {
      console.error('Failed to initialize SharedWorker:', error);
    }
  }

  disconnect() {
    if (this.worker && this.port && this.projectId) {
      if (this.reconnectTimeoutId) {
        clearTimeout(this.reconnectTimeoutId);

        this.reconnectTimeoutId = null;
      }

      if (this.heartbeatIntervalId) {
        clearInterval(this.heartbeatIntervalId);
        this.heartbeatIntervalId = null;
      }

      this.port.postMessage({
        type: 'DISCONNECT',
        projectId: this.projectId,
      } as WorkerMessage);

      this.worker = null;
      this.port = null;

      runInAction(() => {
        this.projectId = null;
        this.cursors.clear();
      });
    } else {
      console.error('Worker or port is not ready');
    }
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
    if (!this.port || !this.projectId) {
      console.error('Worker is not ready');
      return;
    }

    try {
      this.port.postMessage({
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
