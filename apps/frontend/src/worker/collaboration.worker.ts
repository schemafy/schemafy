/// <reference lib="webworker" />

import type { WorkerMessage, WorkerResponse } from './types';

declare const self: SharedWorkerGlobalScope;

const sockets = new Map<string, WebSocket>();
const subscribers = new Map<string, MessagePort[]>();
const portHeartbeats = new Map<MessagePort, number>();

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
        const payload = JSON.parse(event.data);
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
      const message: WorkerResponse = {
        type: 'WS_OPEN',
        projectId,
      };

      port.postMessage(message);
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
