import { Module } from '@nestjs/common';

import { IndexesController } from './indexes.controller';
import { IndexesService } from './indexes.service';

@Module({
    controllers: [IndexesController],
    providers: [IndexesService],
})
export class IndexesModule {}
