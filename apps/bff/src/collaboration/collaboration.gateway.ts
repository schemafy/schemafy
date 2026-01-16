import {
  WebSocketGateway,
  WebSocketServer,
  OnGatewayConnection,
  OnGatewayDisconnect,
} from '@nestjs/websockets';
import { Server, WebSocket as WsWebSocket } from 'ws';
import { IncomingMessage } from 'http';

type WebSocketClient = WsWebSocket & {
  backendWs?: WsWebSocket;
  projectId?: string;
};

@WebSocketGateway({
  path: '/ws/collaboration',
  cors: {
    origin: 'http://localhost:3001',
    credentials: true,
  },
})
export class CollaborationGateway
  implements OnGatewayConnection, OnGatewayDisconnect
{
  @WebSocketServer()
  server: Server;

  handleConnection(client: WebSocketClient, request: IncomingMessage) {
    if (!request.url) {
      client.close(1008, 'Invalid request');
      return;
    }

    const url = new URL(request.url, `http://${request.headers.host}`);
    const projectId = url.searchParams.get('projectId');

    if (!projectId) {
      client.close(1008, 'Project ID is required');
      return;
    }

    client.projectId = projectId;

    const accessToken = this.extractAccessToken(request);

    if (!accessToken) {
      client.close(1008, 'Authentication required');
      return;
    }

    const backendUrl = `ws://localhost:8080/ws/collaboration?projectId=${projectId}`;

    const backendWs = new WsWebSocket(backendUrl, {
      headers: {
        Cookie: `accessToken=${accessToken}`,
      },
    });

    client.backendWs = backendWs;

    backendWs.on('open', () => {
      console.log(`Connected to backend WebSocket for project ${projectId}`);
    });

    backendWs.on('message', (data: Buffer) => {
      if (client.readyState === WsWebSocket.OPEN) {
        client.send(data.toString());
      }
    });

    backendWs.on('error', (error) => {
      console.error('Backend WebSocket error:', error.message);
      console.error('Backend URL:', backendUrl);
    });

    backendWs.on('close', () => {
      console.log(`Backend WebSocket closed for project ${projectId}`);
      if (client.readyState === WsWebSocket.OPEN) {
        client.close(1000, 'Backend connection closed');
      }
    });

    client.on('message', (data: Buffer) => {
      if (backendWs.readyState === WsWebSocket.OPEN) {
        backendWs.send(data.toString());
      }
    });

    console.log(`Client connected to project ${projectId} via BFF proxy`);
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

  handleDisconnect(client: WebSocketClient) {
    if (client.backendWs) {
      if (client.backendWs.readyState === WsWebSocket.OPEN) {
        client.backendWs.close();
      }
      client.backendWs = undefined;
    }

    console.log(`Client disconnected from project ${client.projectId}`);
  }
}
