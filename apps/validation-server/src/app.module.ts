import { Module } from '@nestjs/common';

import { AppController } from './app.controller';
import { AppService } from './app.service';
import { ColumnsModule } from './validations/columns/columns.module';
import { ConstraintsModule } from './validations/constraints/constraints.module';
import { IndexesModule } from './validations/indexes/indexes.module';
import { RelationshipsModule } from './validations/relationships/relationships.module';
import { SchemasModule } from './validations/schemas/schemas.module';
import { TablesModule } from './validations/tables/tables.module';
import { ValidationModule } from './validations/validation/validation.module';

@Module({
  imports: [
    ColumnsModule,
    TablesModule,
    SchemasModule,
    IndexesModule,
    ConstraintsModule,
    RelationshipsModule,
    ValidationModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
