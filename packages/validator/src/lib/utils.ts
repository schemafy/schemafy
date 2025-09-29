import {
  schemaHandlers,
  SchemaHandlers,
  tableHandlers,
  TableHandlers,
  columnHandlers,
  ColumnHandlers,
  indexHandlers,
  IndexHandlers,
  constraintHandlers,
  ConstraintHandlers,
  relationshipHandlers,
  RelationshipHandlers,
} from './handlers';
import { Database } from './types';

interface ERDValidator
  extends SchemaHandlers,
    TableHandlers,
    ColumnHandlers,
    IndexHandlers,
    ConstraintHandlers,
    RelationshipHandlers {
  validate: (database: Database) => void;
}

export const ERD_VALIDATOR: ERDValidator = {
  validate: (database: Database) => {
    return database;
  },
  ...schemaHandlers,
  ...tableHandlers,
  ...columnHandlers,
  ...indexHandlers,
  ...constraintHandlers,
  ...relationshipHandlers,
};
