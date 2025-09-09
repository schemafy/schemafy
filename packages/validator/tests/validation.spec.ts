import { SchemaNameInvalidError, SchemaNameNotUniqueError, SchemaNotExistError } from '../src/lib/errors';
import { Database } from '../src/lib/types';
import { ERD_VALIDATOR } from '../src/lib/utils';

const makeTestDatabaseData = (): Database => {
  return {
    id: '01H8Z7Y5ZKX6G6F4X5Z7Y5ZKX4',
    projects: [
      {
        id: '01H8Z7Y5ZKX6G6F4X5Z7Y5ZKX5',
        projectId: '01H8Z7Y5ZKX6G6F4X5Z7Y5ZKX4',
        dbVendorId: 'mysql',
        name: 'TestSchema1',
        charset: 'utf8mb4',
        collation: 'utf8mb4_general_ci',
        vendorOption: '',
        createdAt: new Date(),
        updatedAt: new Date(),
        deletedAt: null,
      },
      {
        id: '01H8Z7Y5ZKX6G6F4X5Z7Y5ZKX6',
        projectId: '01H8Z7Y5ZKX6G6F4X5Z7Y5ZKX4',
        dbVendorId: 'mysql',
        name: 'TestSchema2',
        charset: 'utf8mb4',
        collation: 'utf8mb4_general_ci',
        vendorOption: '',
        createdAt: new Date(),
        updatedAt: new Date(),
        deletedAt: null,
      },
    ],
  };
};

describe('ERD validation', () => {
  describe('스키마 설정', () => {
    let database: Database;

    beforeEach(() => {
      database = makeTestDatabaseData();
    });

    test('다른 스키마의 이름은 허용된다.', () => {
      const db = ERD_VALIDATOR.changeSchemaName(database, database.projects[1].id, 'TestSchema3');
      expect(db.projects[1].name).toBe('TestSchema3');
    });

    test('존재하지 않는 스키마 ID로 이름 변경은 금지된다.', () => {
      expect(() => {
        ERD_VALIDATOR.changeSchemaName(database, 'invalid-id', 'NewName');
      }).toThrow(SchemaNotExistError);
    });

    test('스키마 이름은 3글자 이상 20글자 이하만 허용된다.', () => {
      expect(() => {
        ERD_VALIDATOR.changeSchemaName(database, database.projects[1].id, 'Te');
      }).toThrow(SchemaNameInvalidError);
    });

    test('같은 데이터베이스(프로젝트) 내 중복된 스키마 이름은 금지된다.', () => {
      expect(() => {
        ERD_VALIDATOR.changeSchemaName(database, database.projects[1].id, 'TestSchema1');
      }).toThrow(SchemaNameNotUniqueError);
    });
  });

  describe('테이블 설정', () => {
    test('같은 스키마 내 중복된 테이블 이름은 금지된다.', () => {
      throw new Error('TODO: duplicate table name test not implemented');
    });
    test('테이블에 PK가 존재하지 않을 수 없다.', () => {
      throw new Error('TODO: table must have primary key test not implemented');
    });
    test('테이블에 PK가 1개 이상 존재할 수 있다.', () => {
      throw new Error('TODO: table can have multiple primary keys test not implemented');
    });
  });

  describe('컬럼 설정', () => {
    test('같은 테이블 내 중복된 컬럼 이름은 금지된다.', () => {
      throw new Error('TODO: duplicate column name test not implemented');
    });
    test('컬럼의 데이터 타입은 반드시 존재해야 한다.', () => {
      throw new Error('TODO: column must have data type test not implemented');
    });
    test('컬럼의 데이터 타입은 데이터베이스 벤더의 유효한 값이어야 한다.', () => {
      throw new Error('TODO: column data type must be valid for the DB vendor test not implemented');
    });
    test('컬럼이 PK인 경우, NULL을 허용하지 않는다.', () => {
      throw new Error('TODO: column with primary key cannot allow NULL test not implemented');
    });
    test('컬럼의 이름은 3글자 이상 40글자 이하만 허용된다.', () => {
      throw new Error('TODO: column name must be between 3 and 40 characters test not implemented');
    });
    test('컬럼의 이름은 영문자, 숫자, 언더바(_)만 허용된다.', () => {
      throw new Error('TODO: column name must be only alphanumeric and underscore test not implemented');
    });
    test('컬럼의 이름은 숫자로 시작할 수 없다.', () => {
      throw new Error('TODO: column name cannot start with a number test not implemented');
    });
  });

  describe('인덱스 설정', () => {
    test('인덱스의 타입은 btree, hash, fulltext, spatial 등 유효한 값이어야 한다', () => {
      throw new Error('TODO: index type must be valid for the DB vendor test not implemented');
    });
    test('하나의 인덱스에 동일한 컬럼이 중복으로 들어갈 수 없다.', () => {
      throw new Error('TODO: index cannot have duplicate columns test not implemented');
    });
    test('PK 인덱스는 반드시 유니크해야 한다.', () => {
      throw new Error('TODO: primary key index must be unique test not implemented');
    });
  });

  describe('인덱스 컬럼 설정', () => {
    test('인덱스 컬럼 순서는 1부터 시작하는 연속된 값이어야 한다.', () => {
      throw new Error('TODO: index column sequence must be consecutive starting from 1 test not implemented');
    });
    test('인덱스 컬럼의 정렬 방향은 asc 또는 desc만 허용된다.', () => {
      throw new Error('TODO: index column sort direction must be asc or desc test not implemented');
    });
    test('인덱스 컬럼의 nulls 설정은 first, last, default 중 하나여야 한다.', () => {
      throw new Error('TODO: index column nulls setting must be valid test not implemented');
    });
  });

  describe('순환 참조가 있는 경우', () => {
    test('직접적인 순환 참조는 에러를 발생시킨다.', () => {
      throw new Error('TODO: direct cyclic reference test not implemented');
    });
    test('간접적인 순환 참조는 허용한다.', () => {
      throw new Error('TODO: indirect cyclic reference test not implemented');
    });
  });
});
