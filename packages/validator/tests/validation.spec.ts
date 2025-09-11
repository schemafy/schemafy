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
  });

  describe('컬럼 설정', () => {
    test('같은 테이블 내 중복된 컬럼 이름은 금지된다.', () => {
      throw new Error('TODO: duplicate column name test not implemented');
    });
    test('컬럼의 데이터 타입은 반드시 존재해야 한다.', () => {
      throw new Error('TODO: column must have data type test not implemented');
    });
    test('컬럼의 데이터 타입은 데이터베이스 벤더에서 유효한 값이어야 한다.', () => {
      throw new Error('TODO: column data type must be valid for the DB vendor test not implemented');
    });
    test('VARCHAR, CHAR 타입은 반드시 길이를 지정해야 한다.', () => {
      throw new Error('TODO: VARCHAR/CHAR must have a length test not implemented');
    });
    test('DECIMAL, NUMERIC 타입은 반드시 정밀도와 스케일을 지정해야 한다.', () => {
      throw new Error('TODO: DECIMAL/NUMERIC must have precision and scale test not implemented');
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
    test('컬럼명으로 해당 데이터베이스 벤더의 예약어를 사용할 수 없다.', () => {
      throw new Error('TODO: column name cannot be a DB vendor reserved keyword test not implemented');
    });
    test('Auto Increment 컬럼은 테이블 당 하나만 존재할 수 있다.', () => {
      throw new Error('TODO: only one auto increment column per table test not implemented');
    });
    test('Auto Increment 컬럼은 숫자 타입의 Primary Key여야 한다.', () => {
      throw new Error('TODO: auto increment column must be a numeric primary key test not implemented');
    });
  });

  describe('인덱스 설정', () => {
    test('인덱스 이름은 테이블 내에서 중복될 수 없다.', () => {
      throw new Error('TODO: index name must be unique within the table test not implemented');
    });
    test('인덱스의 타입은 해당 데이터베이스 벤더에서 지원하는 유효한 값이어야 한다.', () => {
      throw new Error('TODO: index type must be valid for the DB vendor test not implemented');
    });
    test('하나의 인덱스에 동일한 컬럼이 중복으로 들어갈 수 없다.', () => {
      throw new Error('TODO: index cannot have duplicate columns test not implemented');
    });
  });

  describe('인덱스 컬럼 설정', () => {
    test('인덱스 컬럼 순서는 1부터 시작하는 연속된 값이어야 한다.', () => {
      throw new Error('TODO: index column sequence must be consecutive starting from 1 test not implemented');
    });
    test('인덱스 컬럼의 정렬 방향은 asc 또는 desc만 허용된다.', () => {
      throw new Error('TODO: index column sort direction must be asc or desc test not implemented');
    });
  });

  describe('제약 조건 설정', () => {
    test('제약 조건의 이름은 스키마 내에서 고유해야 한다.', () => {
      throw new Error('TODO: constraint name must be unique within the schema test not implemented');
    });

    test('제약 조건은 반드시 하나 이상의 컬럼을 가져야 한다.', () => {
      throw new Error('TODO: constraint must have at least one column test not implemented');
    });

    test('제약 조건에 포함된 컬럼은 해당 제약 조건이 속한 테이블의 컬럼이어야 한다.', () => {
      throw new Error('TODO: constraint columns must belong to the same table test not implemented');
    });

    test('하나의 제약 조건 내에 동일한 컬럼이 중복으로 포함될 수 없다.', () => {
      throw new Error('TODO: constraint cannot contain duplicate columns test not implemented');
    });

    test('테이블에는 반드시 하나의 Primary Key가 존재해야 한다.', () => {
      // ERD 도구의 규칙 상, 모든 테이블은 식별자를 가져야 하므로 PK를 필수로 강제한다.
      throw new Error('TODO: table must have one primary key test not implemented');
    });

    test('하나의 테이블은 여러 컬럼으로 구성된 Primary Key(복합키)를 가질 수 있다.', () => {
      throw new Error('TODO: table can have a composite primary key test not implemented');
    });

    test('Primary Key를 구성하는 모든 컬럼은 NOT NULL 제약 조건이 있어야 한다.', () => {
      throw new Error('TODO: primary key columns must have a NOT NULL constraint test not implemented');
    });

    test('Unique 제약 조건은 Primary Key와 완전히 동일한 컬럼 조합을 가질 수 없다.', () => {
      throw new Error('TODO: unique constraint cannot be same as primary key test not implemented');
    });

    test('서로 다른 Unique 제약 조건이 완전히 동일한 컬럼 조합을 가질 수 없다.', () => {
      throw new Error('TODO: unique constraints cannot have the same column combination test not implemented');
    });

    test('Check 제약 조건은 반드시 check 표현식(check_expr)을 가져야 한다.', () => {
      throw new Error('TODO: check constraint must have an expression test not implemented');
    });

    test('Default 제약 조건은 단 하나의 컬럼에만 적용될 수 있다.', () => {
      throw new Error('TODO: default constraint can only apply to a single column test not implemented');
    });

    test('Default 제약 조건은 반드시 기본값 표현식(default_expr)을 가져야 한다.', () => {
      throw new Error('TODO: default constraint must have an expression test not implemented');
    });
  });

  describe('관계(FK) 설정', () => {
    test('관계는 반드시 하나 이상의 컬럼 쌍을 가져야 한다.', () => {
      throw new Error('TODO: relationship must have at least one column pair test not implemented');
    });

    test('테이블이 자기 자신을 참조하는 관계를 맺을 수 있다.', () => {
      throw new Error('TODO: self-referencing relationship test not implemented');
    });

    test('관계를 맺는 컬럼들은 서로 데이터 타입이 호환되어야 한다.', () => {
      throw new Error('TODO: related columns must have compatible data types test not implemented');
    });

    test('참조 대상(Target) 컬럼들은 해당 테이블의 Primary Key 또는 Unique 제약 조건이어야 한다.', () => {
      // Foreign Key의 대상이 되는 컬럼(들)은 데이터의 고유성을 보장해야만 참조 무결성을 지킬 수 있다.
      // 따라서, 참조받는 테이블의 PK나 UK 제약이 걸린 컬럼(들)만 FK의 대상이 될 수 있다.
      throw new Error('TODO: foreign key target must be a primary or unique key test not implemented');
    });

    test('식별 관계(Identifying Relationship)의 자식 테이블 컬럼(FK)은 자식 테이블 Primary Key의 일부여야 한다.', () => {
      // 식별 관계는 부모 테이블의 PK가 자식 테이블의 PK이자 FK가 되는 관계이다.
      // 이 테스트는 자식 테이블의 FK로 지정된 컬럼이, 실제로 자식 테이블의 PK 제약 조건에도 포함되어 있는지 검증한다.
      /*
         검증 로직
           1. DB_RELATIONSHIPS에서 kind가 'identifying'인 관계를 찾는다.
           2. 해당 관계의 src_table_id를 기준으로 DB_CONSTRAINTS에서 kind가 'primary_key'인 제약 조건을 찾는다.
           3. DB_CONSTRAINT_COLUMNS에서 해당 PK에 속한 컬럼 ID 목록을 가져온다. (A)
           4. DB_RELATIONSHIP_COLUMNS에서 해당 관계의 src_column_id 목록을 가져온다. (B)
           5. B 목록의 모든 컬럼이 A 목록에 포함되어 있는지 확인한다.
         */
      throw new Error('TODO: identifying relationship FK must be part of child PK test not implemented');
    });

    test('식별 관계의 컬럼은 PK 제약 조건과 FK 관계 정의에 모두 존재해야 한다.', () => {
      // 식별 관계의 컬럼은 PK이자 FK이므로, 이 두 가지 역할을 나타내는 데이터가 모두 존재해야 한다.
      // 이 테스트는 특정 컬럼이 DB_CONSTRAINT_COLUMNS (PK 역할)와 DB_RELATIONSHIP_COLUMNS (FK 역할) 양쪽에
      // 모두 올바르게 등록되어 있는지 교차 검증한다.
      throw new Error('TODO: identifying relationship column must be in PK and FK definitions test not implemented');
    });

    test('관계의 ON DELETE 옵션이 "SET NULL"인 경우, 해당 외래 키 컬럼은 NULL을 허용해야 한다.', () => {
      throw new Error('TODO: ON DELETE SET NULL requires nullable column test not implemented');
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
