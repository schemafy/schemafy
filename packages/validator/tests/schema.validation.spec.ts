import { createTestDatabase } from '../src/lib/builder';
import { SchemaNameInvalidError, SchemaNameNotUniqueError, SchemaNotExistError, ERD_VALIDATOR } from '../src';
import type { Database } from '../src/lib/types';

describe('Schema validation', () => {
  let database: Database;

  beforeEach(() => {
    database = createTestDatabase()
      .withSchema((schema) => schema.withName('TestSchema1').withId('schema-1'))
      .withSchema((schema) => schema.withName('TestSchema2').withId('schema-2'))
      .build();
  });

  test.skip('다른 스키마의 이름은 허용된다.', () => {
    const db = ERD_VALIDATOR.changeSchemaName(database, 'schema-2', 'TestSchema3');
    expect(db.schemas[1].name).toBe('TestSchema3');
  });

  test.skip('존재하지 않는 스키마 ID로 이름 변경은 금지된다.', () => {
    expect(() => {
      ERD_VALIDATOR.changeSchemaName(database, 'non-existent-schema', 'NewName');
    }).toThrow(SchemaNotExistError);
  });

  test.skip('스키마 이름은 3글자 이상 20글자 이하만 허용된다.', () => {
    expect(() => {
      ERD_VALIDATOR.changeSchemaName(database, 'schema-2', 'ab'); // 너무 짧음
    }).toThrow(SchemaNameInvalidError);
  });

  test.skip('같은 데이터베이스(프로젝트) 내 중복된 스키마 이름은 금지된다.', () => {
    expect(() => {
      ERD_VALIDATOR.changeSchemaName(database, 'schema-2', 'TestSchema1');
    }).toThrow(SchemaNameNotUniqueError);
  });

  test.skip('빈 스키마를 삭제할 수 있다', () => {
    expect(() => ERD_VALIDATOR.deleteSchema(database, 'schema-1')).not.toThrow();
  });

  test.skip('존재하지 않는 스키마 삭제 시 에러 발생', () => {
    expect(() => ERD_VALIDATOR.deleteSchema(database, 'non-existent')).toThrow(SchemaNotExistError);
  });

  test.skip('테이블이 있는 스키마도 삭제 가능하다', () => {
    const dbWithTable = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withName('TestSchema1')
          .withTable((t) => t.withId('table-1').withName('users'))
      )
      .withSchema((s) => s.withId('schema-2').withName('TestSchema2'))
      .build();

    expect(() => ERD_VALIDATOR.deleteSchema(dbWithTable, 'schema-1')).not.toThrow();
  });

  test.skip('마지막 스키마 삭제 시 에러 발생', () => {
    const singleSchemaDb = createTestDatabase()
      .withSchema((s) => s.withId('schema-1').withName('TestSchema1'))
      .build();

    expect(() => ERD_VALIDATOR.deleteSchema(singleSchemaDb, 'schema-1')).toThrow();
  });
});
