import { Injectable } from '@nestjs/common';
import { BackendClientService } from '../common/backend-client/backend-client.service';

@Injectable()
export class ErdService {
  constructor(private readonly backendClient: BackendClientService) {}
}
