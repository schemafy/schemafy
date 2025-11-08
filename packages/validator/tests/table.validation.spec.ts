import { ERD_VALIDATOR } from '../src/lib/utils';
import { TableNameNotUniqueError, TableNotExistError } from '../src/lib/errors';
import { createTestDatabase } from '../src/lib/builder';
import type { Table } from '../src/lib/types';

describe('Table validation', () => {
  test('새 테이블 생성 시, 같은 스키마 내에 중복된 이름이 있으면 에러를 발생시킨다', () => {
    const schemaId = 'schema-1';
    const table: Omit<Table, 'schemaId' | 'createdAt' | 'updatedAt'> = {
      id: 'table-1',
      name: 'posts',
      columns: [],
      constraints: [],
      indexes: [],
      relationships: [],
      comment: '',
      tableOptions: '',
      isAffected: false,
    };

    const database = createTestDatabase()
      .withSchema((schema) =>
        schema
          .withId(schemaId)
          .withName('TestSchema')
          .withTable((t) => t.withId('table-1').withName('posts'))
      )
      .build();

    expect(() => ERD_VALIDATOR.createTable(database, schemaId, table)).toThrow(TableNameNotUniqueError);
  });

  test('테이블 이름 변경 시, 같은 스키마 내에 중복된 이름이 있으면 에러를 발생시킨다', () => {
    const schemaId = 'schema-1';
    const table: Omit<Table, 'schemaId' | 'createdAt' | 'updatedAt'> = {
      id: 'table-1',
      name: 'users',
      columns: [],
      constraints: [],
      indexes: [],
      relationships: [],
      comment: '',
      tableOptions: '',
      isAffected: false,
    };

    const database = createTestDatabase()
      .withSchema((schema) =>
        schema
          .withId(schemaId)
          .withName('TestSchema')
          .withTable((t) => t.withId('table-1').withName('posts'))
      )
      .build();

    const dbWithUsers = ERD_VALIDATOR.createTable(database, schemaId, table);
    expect(() => ERD_VALIDATOR.changeTableName(dbWithUsers, schemaId, 'table-1', 'users')).toThrow(
      TableNameNotUniqueError
    );
  });

  test('다른 스키마에 있는 테이블 이름과는 중복되어도 괜찮다', () => {
    const schemaId = 'schema-1';
    const table: Omit<Table, 'schemaId' | 'createdAt' | 'updatedAt'> = {
      id: 'table-1',
      name: 'users',
      columns: [],
      constraints: [],
      indexes: [],
      relationships: [],
      comment: '',
      tableOptions: '',
      isAffected: false,
    };

    const anotherSchemaId = 'schema-2';
    let dbWithAnotherSchema = createTestDatabase()
      .withSchema((schema) =>
        schema
          .withId(schemaId)
          .withName('TestSchema1')
          .withTable((t) => t.withId('table-1').withName('users'))
      )
      .withSchema((schema) => schema.withId(anotherSchemaId).withName('TestSchema2'))
      .build();

    expect(() => ERD_VALIDATOR.createTable(dbWithAnotherSchema, anotherSchemaId, table)).not.toThrow();
  });

  test('독립적인 테이블을 삭제할 수 있다', () => {
    const simpleDb = createTestDatabase()
      .withSchema((s) => s.withId('schema-1').withTable((t) => t.withId('table-1').withName('simple')))
      .build();

    expect(() => ERD_VALIDATOR.deleteTable(simpleDb, 'schema-1', 'table-1')).not.toThrow();
  });

  test('존재하지 않는 테이블 삭제 시 에러 발생', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) =>
            t
              .withId('parent-table')
              .withName('parent')
              .withColumn((c) => c.withId('parent-id').withName('id2').withDataType('INT'))
          )
          .withTable((t) =>
            t
              .withId('child-table')
              .withName('child')
              .withColumn((c) => c.withId('child-parent-id').withName('parent_id').withDataType('INT'))
          )
      )
      .build();

    expect(() => ERD_VALIDATOR.deleteTable(database, 'schema-1', 'non-existent')).toThrow(TableNotExistError);
  });

  test('관계로 참조되는 부모 테이블도 삭제 가능하다', () => {
    const dbWithRelation = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) =>
            t
              .withId('parent-table')
              .withName('parent')
              .withColumn((c) => c.withId('parent-id').withName('id2').withDataType('INT'))
              .withConstraint((c) =>
                c
                  .withName('pk_parent')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId('parent-id'))
              )
              .withRelationship((r) =>
                r
                  .withName('fk_child_parent')
                  .withKind('NON_IDENTIFYING')
                  .withTgtTableId('parent-table')
                  .withColumn((rc) => rc.withFkColumnId('child-parent-id').withRefColumnId('parent-id'))
              )
          )
          .withTable((t) =>
            t
              .withId('child-table')
              .withName('child')
              .withColumn((c) => c.withId('child-parent-id').withName('parent_id').withDataType('INT'))
              .withRelationship((r) =>
                r
                  .withName('fk_child_parent')
                  .withKind('NON_IDENTIFYING')
                  .withTgtTableId('parent-table')
                  .withColumn((rc) => rc.withFkColumnId('child-parent-id').withRefColumnId('parent-id'))
              )
          )
      )
      .build();

    const schemaBefore = dbWithRelation.schemas.find((s) => s.id === 'schema-1');
    const parentBefore = schemaBefore?.tables.find((t) => t.id === 'parent-table');
    const childBefore = schemaBefore?.tables.find((t) => t.id === 'child-table');

    expect(parentBefore).toBeDefined();
    expect(childBefore).toBeDefined();
    expect(parentBefore?.relationships.length).toBeGreaterThan(0);
    expect(childBefore?.relationships.length).toBeGreaterThan(0);

    expect(() => ERD_VALIDATOR.deleteTable(dbWithRelation, 'schema-1', 'parent-table')).not.toThrow();
  });

  test('FK를 가진 자식 테이블은 삭제 가능하다', () => {
    const dbWithFK = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) =>
            t
              .withId('parent-table')
              .withName('parent')
              .withColumn((c) => c.withId('parent-id').withName('id2'))
              .withRelationship((r) =>
                r
                  .withName('fk_child_parent')
                  .withKind('NON_IDENTIFYING')
                  .withTgtTableId('parent-table')
                  .withColumn((rc) => rc.withFkColumnId('child-parent-id').withRefColumnId('parent-id'))
              )
          )
          .withTable((t) =>
            t
              .withId('child-table')
              .withName('child')
              .withColumn((c) => c.withId('child-parent-id').withName('parent_id'))
              .withRelationship((r) =>
                r
                  .withName('fk_child_parent')
                  .withKind('NON_IDENTIFYING')
                  .withTgtTableId('parent-table')
                  .withColumn((rc) => rc.withFkColumnId('child-parent-id').withRefColumnId('parent-id'))
              )
          )
      )
      .build();

    expect(() => ERD_VALIDATOR.deleteTable(dbWithFK, 'schema-1', 'child-table')).not.toThrow();
  });
});
