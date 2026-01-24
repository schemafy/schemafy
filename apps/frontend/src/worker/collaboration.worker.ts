/// <reference lib="webworker" />

import type {
  CursorPosition,
  WebSocketMessage,
} from '../lib/api/collaboration/types';
import type { UserInfo, WorkerMessage, WorkerResponse } from './types';

declare const self: SharedWorkerGlobalScope;

const sockets = new Map<string, WebSocket>();
const subscribers = new Map<string, MessagePort[]>();
const portHeartbeats = new Map<MessagePort, number>();

type ProjectState = {
  cursors: Map<string, CursorPosition>;
  users: Map<string, UserInfo>;
};
const projectStates = new Map<string, ProjectState>();

const WEBSOCKET_URL =
  import.meta.env.VITE_BFF_WS_URL || 'ws://localhost:4000/ws/collaboration';

const HEARTBEAT_INTERVAL = 30000;
const HEARTBEAT_TIMEOUT = 60000;

setInterval(() => {
  const now = Date.now();
  portHeartbeats.forEach((lastBeat, port) => {
    if (now - lastBeat > HEARTBEAT_TIMEOUT) {
      closePort(port);
    }
  });
}, HEARTBEAT_INTERVAL);

function closePort(port: MessagePort) {
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

self.onconnect = (e: MessageEvent) => {
  const port = e.ports[0];

  portHeartbeats.set(port, Date.now());

  port.addEventListener('message', (event: MessageEvent<WorkerMessage>) => {
    portHeartbeats.set(port, Date.now());

    const { type } = event.data;

    if (type === 'CONNECT') {
      const { projectId } = event.data;
      handleConnect(projectId, port);
    } else if (type === 'DISCONNECT') {
      closePort(port);
    } else if (type === 'SEND_MESSAGE') {
      const { projectId, payload } = event.data;
      const ws = sockets.get(projectId);
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(payload));
      } else {
        console.warn(
          '[SharedWorker] WebSocket not ready, state:',
          ws?.readyState,
        );
      }
    }
  });

  port.start();
};

function handleConnect(projectId: string, port: MessagePort) {
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
            state.users.set(payload.userId, {
              userId: payload.userId,
              userName: payload.userName,
            });
          } else if (payload.type === 'LEAVE') {
            state.users.delete(payload.userId);
            state.cursors.delete(payload.userId);
          } else if (payload.type === 'CURSOR') {
            state.cursors.set(payload.userInfo.userId, {
              userId: payload.userInfo.userId,
              userName: payload.userInfo.userName,
              x: payload.cursor.x,
              y: payload.cursor.y,
            });
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
