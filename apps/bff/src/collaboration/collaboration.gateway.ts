import {
  WebSocketGateway,
  WebSocketServer,
  OnGatewayConnection,
  OnGatewayDisconnect,
} from '@nestjs/websockets';
import { Server, WebSocket } from 'ws';
import { IncomingMessage } from 'http';

type WebSocketClient = WebSocket & {
  backendWs?: WebSocket;
  projectId?: string;
};

const WS_CLOSE_NORMAL = 1000;
const WS_CLOSE_POLICY_VIOLATION = 1008;

@WebSocketGateway({
  path: '/ws/collaboration',
  cors: {
    origin: process.env.FRONTEND_URL || 'http://localhost:3001',
    credentials: true,
  },
})
export class CollaborationGateway
  implements OnGatewayConnection, OnGatewayDisconnect
{
  @WebSocketServer()
  server: Server;

  constructor() {}

  handleConnection(client: WebSocketClient, request: IncomingMessage) {
    const validationResult = this.validateRequest(client, request);
    if (!validationResult) {
      return;
    }

    const { projectId, accessToken } = validationResult;
    client.projectId = projectId;

    this.setupBackendConnection(client, projectId, accessToken);
  }

  handleDisconnect(client: WebSocketClient) {
    if (client.backendWs) {
      if (client.backendWs.readyState === WebSocket.OPEN) {
        client.backendWs.close();
      }
      client.backendWs = undefined;
    }
  }

  private validateRequest(
    client: WebSocketClient,
    request: IncomingMessage,
  ): { projectId: string; accessToken: string } | null {
    if (!request.url) {
      client.close(WS_CLOSE_POLICY_VIOLATION, 'Invalid request');
      return null;
    }

    const url = new URL(request.url, `http://${request.headers.host}`);
    const projectId = url.searchParams.get('projectId');

    if (!projectId) {
      client.close(WS_CLOSE_POLICY_VIOLATION, 'Project ID is required');
      return null;
    }

    const accessToken = this.extractAccessToken(request);

    if (!accessToken) {
      client.close(WS_CLOSE_POLICY_VIOLATION, 'Authentication required');
      return null;
    }

    return { projectId, accessToken };
  }

  private setupBackendConnection(
    client: WebSocketClient,
    projectId: string,
    accessToken: string,
  ) {
    const backendBaseUrl =
      process.env.BACKEND_WS_URL || 'ws://localhost:8080/ws/collaboration';
    const backendUrl = `${backendBaseUrl}?projectId=${projectId}`;
    const backendWs = new WebSocket(backendUrl, {
      headers: {
        Cookie: `accessToken=${accessToken}`,
      },
    });

    client.backendWs = backendWs;

    backendWs.on('open', () => {
      console.log(`Connected to backend WebSocket for project ${projectId}`);
    });

    backendWs.on('message', (data: Buffer) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data.toString());
      }
    });

    backendWs.on('error', (error) => {
      console.error('Backend WebSocket error:', error.message);
    });

    backendWs.on('close', () => {
      console.log(`Backend WebSocket closed for project ${projectId}`);
      if (client.readyState === WebSocket.OPEN) {
        client.close(WS_CLOSE_NORMAL, 'Backend connection closed');
      }
    });

    client.on('message', (data: Buffer) => {
      if (backendWs.readyState === WebSocket.OPEN) {
        backendWs.send(data.toString());
      }
    });
  }

  private extractAccessToken(request: IncomingMessage): string | null {
    const cookieHeader = request.headers.cookie;

    if (!cookieHeader) {
      return null;
    }

    const cookies = cookieHeader.split(';').reduce(
      (acc, cookie) => {
        const [key, value] = cookie.trim().split('=');
        acc[key] = value;
        return acc;
      },
      {} as Record<string, string>,
    );

    return cookies.accessToken || null;
  }
}
