import { Controller, Get } from '@nestjs/common';
import axios from 'axios';

type HealthStatus = 'up' | 'down';

type HealthResponse = {
  status: 'ok' | 'unhealthy';
  backend: HealthStatus;
};

@Controller('health')
export class HealthController {
  private readonly backendUrl = process.env.BACKEND_URL ?? 'http://localhost:8080';

  @Get()
  async check(): Promise<HealthResponse> {
    const backend = await this.checkBackend();

    return {
      status: backend === 'up' ? 'ok' : 'unhealthy',
      backend,
    };
  }

  private async checkBackend(): Promise<HealthStatus> {
    try {
      await axios.get(`${this.backendUrl}/actuator/health`, { timeout: 3000 });
      return 'up';
    } catch {
      return 'down';
    }
  }
}
