import { Controller } from '@nestjs/common';
import { ErdService } from './erd.service';

@Controller('api/v1.0')
export class ErdController {
  constructor(private readonly erdService: ErdService) {}
}
