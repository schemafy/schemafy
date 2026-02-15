import { Module } from '@nestjs/common';
import { SchemaController } from './schema.controller';
import { SchemaService } from './schema.service';
import { TableController } from './table.controller';
import { TableService } from './table.service';
import { ColumnController } from './column.controller';
import { ColumnService } from './column.service';
import { IndexController } from './index.controller';
import { IndexService } from './index.service';
import { ConstraintController } from './constraint.controller';
import { ConstraintService } from './constraint.service';
import { RelationshipController } from './relationship.controller';
import { RelationshipService } from './relationship.service';

@Module({
  controllers: [
    SchemaController,
    TableController,
    ColumnController,
    IndexController,
    ConstraintController,
    RelationshipController,
  ],
  providers: [
    SchemaService,
    TableService,
    ColumnService,
    IndexService,
    ConstraintService,
    RelationshipService,
  ],
})
export class ErdModule {}
