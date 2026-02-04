/// <reference lib="webworker" />

import type {
  CursorPosition,
  WebSocketMessage,
} from '../lib/api/collaboration/types';
import type { UserInfo, WorkerMessage, WorkerResponse } from './types';

declare const self: SharedWorkerGlobalScope | DedicatedWorkerGlobalScope;

type WorkerPort = MessagePort | DedicatedWorkerGlobalScope;

const SHARED_WORKER_ENABLE = typeof SharedWorkerGlobalScope !== 'undefined';

const sockets = new Map<string, WebSocket>();
const subscribers = new Map<string, WorkerPort[]>();
const portUserInfos = new Map<WorkerPort, UserInfo>();
const portHeartbeats = new Map<WorkerPort, number>();

type ProjectState = {
  cursors: Map<string, CursorPosition>;
  users: Map<string, UserInfo>;
};
const projectStates = new Map<string, ProjectState>();

const WEBSOCKET_URL =
  import.meta.env.VITE_BFF_WS_URL || 'ws://localhost:4000/ws/collaboration';

const HEARTBEAT_INTERVAL = 30000;
const HEARTBEAT_TIMEOUT = 60000;

// SharedWorker에서는 탭 닫힘같은 이벤트를 인식하지 못하기 때문에 주기적으로 제거하면서
// 메모리 누수를 막는다
setInterval(() => {
  const now = Date.now();
  portHeartbeats.forEach((lastBeat, port) => {
    if (now - lastBeat > HEARTBEAT_TIMEOUT) {
      closePort(port);
    }
  });
}, HEARTBEAT_INTERVAL);

function closePort(port: WorkerPort) {
  portHeartbeats.delete(port);

  subscribers.forEach((ports, projectId) => {
    const nextPorts = ports.filter((p) => p !== port);
    subscribers.set(projectId, nextPorts);

    if (nextPorts.length === 0) {
      const ws = sockets.get(projectId);

      if (ws) {
        ws.close();
        sockets.delete(projectId);
      }

      subscribers.delete(projectId);
      projectStates.delete(projectId);
    }
  });
}

function handlePort(port: WorkerPort) {
  portHeartbeats.set(port, Date.now());

  const onMessage = (event: Event) => {
    const messageEvent = event as MessageEvent<WorkerMessage>;
    portHeartbeats.set(port, Date.now());

    const { type } = messageEvent.data;

    if (type === 'CONNECT') {
      const { projectId, userInfo } = messageEvent.data;
      portUserInfos.set(port, userInfo);

      handleConnect(projectId, port);
    } else if (type === 'DISCONNECT') {
      portUserInfos.delete(port);

      closePort(port);
    } else if (type === 'SEND_MESSAGE') {
      const { projectId, payload } = messageEvent.data;

      if (payload.type === 'CURSOR') {
        const userInfo = portUserInfos.get(port);
        const state = projectStates.get(projectId);

        if (userInfo && state) {
          state.cursors.set(userInfo.userId, {
            userId: userInfo.userId,
            userName: userInfo.userName,
            x: payload.cursor.x,
            y: payload.cursor.y,
          });
        }
      }

      const ws = sockets.get(projectId);

      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(payload));
      } else {
        console.warn('[Worker] WebSocket not ready, state:', ws?.readyState);
      }
    }
  };

  port.addEventListener('message', onMessage);

  if (port instanceof MessagePort) {
    port.start();
  }
}

if (SHARED_WORKER_ENABLE && self instanceof SharedWorkerGlobalScope) {
  console.log('[Worker] SharedWorkerGlobalScope');
  self.onconnect = (e: MessageEvent) => {
    const port = e.ports[0];
    handlePort(port);
  };
} else {
  handlePort(self as DedicatedWorkerGlobalScope);
}

function handleConnect(projectId: string, port: WorkerPort) {
  if (!subscribers.has(projectId)) {
    subscribers.set(projectId, []);
  }

  const ports = subscribers.get(projectId)!;

  if (!ports.includes(port)) {
    ports.push(port);
  }

  if (!projectStates.has(projectId)) {
    projectStates.set(projectId, {
      cursors: new Map(),
      users: new Map(),
    });
  }

  if (!sockets.has(projectId)) {
    const wsUrl = `${WEBSOCKET_URL}?projectId=${projectId}`;

    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      const message: WorkerResponse = {
        type: 'WS_OPEN',
        projectId,
      };

      broadcast(projectId, message);
    };

    ws.onmessage = (event) => {
      try {
        const payload: WebSocketMessage = JSON.parse(event.data);
        const state = projectStates.get(projectId);

        if (state) {
          if (payload.type === 'JOIN') {
            const userInfo: UserInfo = {
              userId: payload.userId,
              userName: payload.userName,
            };

            const cursorInfo: CursorPosition = {
              userId: payload.userId,
              userName: payload.userName,
              x: 0,
              y: 0,
            };

            state.users.set(payload.userId, userInfo);
            state.cursors.set(payload.userId, cursorInfo);
          } else if (payload.type === 'LEAVE') {
            state.users.delete(payload.userId);
            state.cursors.delete(payload.userId);
          } else if (payload.type === 'CURSOR') {
            const cursorInfo: CursorPosition = {
              userId: payload.userInfo.userId,
              userName: payload.userInfo.userName,
              x: payload.cursor.x,
              y: payload.cursor.y,
            };

            state.cursors.set(payload.userInfo.userId, cursorInfo);
          } else if (payload.type === 'CHAT') {
            const cursor = state.cursors.get(payload.userId);

            if (cursor) {
              payload.position = { x: cursor.x, y: cursor.y };
            }
          }
        }

        const message: WorkerResponse = {
          type: 'WS_MESSAGE',
          projectId,
          payload,
        };

        broadcast(projectId, message);
      } catch (error) {
        console.error('[SharedWorker] Parse error', error);
      }
    };

    ws.onclose = () => {
      const message: WorkerResponse = {
        type: 'WS_CLOSE',
        projectId,
      };

      broadcast(projectId, message);
      sockets.delete(projectId);
    };

    ws.onerror = (event) => {
      console.error('[SharedWorker] WebSocket ERROR:', event);

      const message: WorkerResponse = {
        type: 'WS_ERROR',
        projectId,
        error: 'WebSocket error occurred',
      };

      broadcast(projectId, message);
    };

    sockets.set(projectId, ws);
  } else {
    const ws = sockets.get(projectId);
    if (ws?.readyState === WebSocket.OPEN) {
      const openMessage: WorkerResponse = {
        type: 'WS_OPEN',
        projectId,
      };

      port.postMessage(openMessage);

      const state = projectStates.get(projectId);

      if (state) {
        const initialMessage: WorkerResponse = {
          type: 'INITIAL_STATE',
          projectId,
          cursors: Array.from(state.cursors.values()),
          users: Array.from(state.users.values()),
        };

        port.postMessage(initialMessage);
      }
    }
  }
}

function broadcast(projectId: string, message: WorkerResponse) {
  const ports = subscribers.get(projectId);
  if (ports) {
    ports.forEach((port) => {
      try {
        port.postMessage(message);
      } catch (e: unknown) {
        console.error(e);
        closePort(port);
      }
    });
  }
}
