import { CallHandler, ExecutionContext } from '@nestjs/common';
import { of } from 'rxjs';

import { ProtoTransformInterceptor } from './proto-transform.interceptor';

describe('ProtoTransformInterceptor', () => {
  let interceptor: ProtoTransformInterceptor;
  let mockExecutionContext: ExecutionContext;
  let mockCallHandler: CallHandler;

  beforeEach(() => {
    interceptor = new ProtoTransformInterceptor();

    mockExecutionContext = {
      switchToRpc: jest.fn().mockReturnValue({
        getData: jest.fn(),
      }),
    } as unknown as ExecutionContext;

    mockCallHandler = {
      handle: jest.fn().mockReturnValue(of({})),
    };
  });

  describe('Field transformations', () => {
    it('should transform date fields', () => {
      const data = {
        createdAt: '2025-10-08T10:30:00.000Z',
        deletedAt: '',
      };

      (mockExecutionContext.switchToRpc().getData as jest.Mock).mockReturnValue(data);

      interceptor.intercept(mockExecutionContext, mockCallHandler);

      expect(data.createdAt).toBeInstanceOf(Date);
      expect(data.deletedAt).toBeNull();
    });

    it('should transform nullable string fields', () => {
      const data = {
        dataType: 'VARCHAR',
        comment: '',
      };

      (mockExecutionContext.switchToRpc().getData as jest.Mock).mockReturnValue(data);

      interceptor.intercept(mockExecutionContext, mockCallHandler);

      expect(data.dataType).toBe('VARCHAR');
      expect(data.comment).toBeNull();
    });

    it('should transform enum fields', () => {
      const data = {
        dbVendorId: 'MYSQL',
        type: 'BTREE',
        sortDir: 'ASC',
        kind: 'PRIMARY_KEY',
        onDelete: 'CASCADE',
        onUpdate: 'CASCADE_UPDATE',
        cardinality: 'ONE_TO_ONE',
      };

      (mockExecutionContext.switchToRpc().getData as jest.Mock).mockReturnValue(data);

      interceptor.intercept(mockExecutionContext, mockCallHandler);

      expect(data.dbVendorId).toBe('mysql');
      expect(data.type).toBe('BTREE');
      expect(data.sortDir).toBe('ASC');
      expect(data.kind).toBe('PRIMARY_KEY');
      expect(data.onDelete).toBe('CASCADE');
      expect(data.onUpdate).toBe('CASCADE');
      expect(data.cardinality).toBe('1:1');
    });

    it('should validate fkEnforced must be false', () => {
      const data = {
        fkEnforced: true,
      };

      (mockExecutionContext.switchToRpc().getData as jest.Mock).mockReturnValue(data);

      expect(() => interceptor.intercept(mockExecutionContext, mockCallHandler)).toThrow('fkEnforced must be false');
    });
  });

  describe('Nested transformations', () => {
    it('should transform nested objects', () => {
      const data = {
        schema: {
          dbVendorId: 'MYSQL',
          createdAt: '2025-10-08T10:30:00.000Z',
        },
      };

      (mockExecutionContext.switchToRpc().getData as jest.Mock).mockReturnValue(data);

      interceptor.intercept(mockExecutionContext, mockCallHandler);

      expect(data.schema.dbVendorId).toBe('mysql');
      expect(data.schema.createdAt).toBeInstanceOf(Date);
    });

    it('should transform arrays', () => {
      const data = {
        columns: [{ createdAt: '2025-10-08T10:30:00.000Z' }, { createdAt: '2025-10-08T10:30:00.000Z' }],
      };

      (mockExecutionContext.switchToRpc().getData as jest.Mock).mockReturnValue(data);

      interceptor.intercept(mockExecutionContext, mockCallHandler);

      expect(data.columns[0].createdAt).toBeInstanceOf(Date);
      expect(data.columns[1].createdAt).toBeInstanceOf(Date);
    });
  });

  describe('Complete structure transformation', () => {
    it('should transform full database structure', () => {
      const data = {
        database: {
          schemas: [
            {
              dbVendorId: 'MYSQL',
              createdAt: '2025-10-08T10:30:00.000Z',
              tables: [
                {
                  name: 'users',
                  comment: '',
                  createdAt: '2025-10-08T10:30:00.000Z',
                  columns: [
                    {
                      name: 'id',
                      dataType: 'INT',
                      comment: '',
                      createdAt: '2025-10-08T10:30:00.000Z',
                    },
                  ],
                  indexes: [
                    {
                      type: 'BTREE',
                      comment: '',
                      columns: [{ sortDir: 'ASC' }],
                    },
                  ],
                  constraints: [
                    {
                      kind: 'PRIMARY_KEY',
                      checkExpr: '',
                    },
                  ],
                  relationships: [
                    {
                      kind: 'IDENTIFYING',
                      cardinality: 'ONE_TO_ONE',
                      onDelete: 'CASCADE',
                      onUpdate: 'CASCADE_UPDATE',
                      fkEnforced: false,
                    },
                  ],
                },
              ],
            },
          ],
        },
      };

      (mockExecutionContext.switchToRpc().getData as jest.Mock).mockReturnValue(data);

      interceptor.intercept(mockExecutionContext, mockCallHandler);

      const schema = data.database.schemas[0];
      const table = schema.tables[0];
      const column = table.columns[0];
      const index = table.indexes[0];
      const constraint = table.constraints[0];
      const relationship = table.relationships[0];

      expect(schema.dbVendorId).toBe('mysql');
      expect(schema.createdAt).toBeInstanceOf(Date);
      expect(table.comment).toBeNull();
      expect(column.dataType).toBe('INT');
      expect(column.comment).toBeNull();
      expect(index.type).toBe('BTREE');
      expect(index.columns[0].sortDir).toBe('ASC');
      expect(constraint.kind).toBe('PRIMARY_KEY');
      expect(constraint.checkExpr).toBeNull();
      expect(relationship.cardinality).toBe('1:1');
      expect(relationship.onUpdate).toBe('CASCADE');
    });
  });

  describe('Error handling', () => {
    it('should include field path in error messages', () => {
      const data = {
        schema: {
          dbVendorId: 'INVALID',
        },
      };

      (mockExecutionContext.switchToRpc().getData as jest.Mock).mockReturnValue(data);

      expect(() => interceptor.intercept(mockExecutionContext, mockCallHandler)).toThrow(
        "Error transforming field 'schema.dbVendorId'",
      );
    });

    it('should include array indices in error paths', () => {
      const data = {
        columns: [{ createdAt: 'invalid-date' }],
      };

      (mockExecutionContext.switchToRpc().getData as jest.Mock).mockReturnValue(data);

      expect(() => interceptor.intercept(mockExecutionContext, mockCallHandler)).toThrow(
        "Error transforming field 'columns[0].createdAt'",
      );
    });
  });
});
