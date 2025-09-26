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

interface ERDValidator
  extends SchemaHandlers,
    TableHandlers,
    ColumnHandlers,
    IndexHandlers,
    ConstraintHandlers,
    RelationshipHandlers {}

export const ERD_VALIDATOR: ERDValidator = {
  ...schemaHandlers,
  ...tableHandlers,
  ...columnHandlers,
  ...indexHandlers,
  ...constraintHandlers,
  ...relationshipHandlers,
};
