import { createConstraintBuilder, createTestDatabase } from '../src/lib/builder';
import {
  ConstraintColumnNotExistError,
  ConstraintNameNotUniqueError,
  ConstraintColumnNotUniqueError,
  UniqueSameAsPrimaryKeyError,
  DuplicateKeyDefinitionError,
  ConstraintNotExistError,
} from '../src/lib/errors';
import { ERD_VALIDATOR } from '../src/lib/utils';

describe('Constraint validation', () => {
  test('제약 조건의 이름은 스키마 내에서 고유해야 한다', () => {
    const newConstraint = createConstraintBuilder()
      .withName('pk_users')
      .withKind('PRIMARY_KEY')
      .withColumn((cc) => cc.withColumnId('id-col'))
      .build();

    const db = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) =>
            t
              .withId('table-1')
              .withName('posts')
              .withColumn((c) => c.withId('id-col').withName('id'))
              .withConstraint((c) =>
                c
                  .withName('pk_users')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId('id-col'))
              )
          )
          .withTable((t) => t.withId('table-2').withName('users'))
      )
      .build();

    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-2', newConstraint)).toThrow(
      ConstraintNameNotUniqueError
    );
  });

  test('제약 조건에 포함된 컬럼은 해당 제약 조건이 속한 테이블의 컬럼이어야 한다', () => {
    const newConstraint = createConstraintBuilder()
      .withName('pk_invalid_column')
      .withKind('PRIMARY_KEY')
      .withColumn((cc) => cc.withColumnId('non_existent_column'))
      .build();

    const db = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) => t.withId('table-1').withColumn((c) => c.withId('id-col').withName('id')))
      )
      .build();

    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-1', newConstraint)).toThrow(
      ConstraintColumnNotExistError
    );
  });

  test('하나의 제약 조건 내에 동일한 컬럼이 중복으로 포함될 수 없다', () => {
    const newConstraint = createConstraintBuilder()
      .withName('pk_duplicate_column')
      .withKind('PRIMARY_KEY')
      .withColumn((cc) => cc.withColumnId('id-col'))
      .withColumn((cc) => cc.withColumnId('id-col'))
      .build();

    const db = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) => t.withId('table-1').withColumn((c) => c.withId('id-col').withName('id')))
      )
      .build();

    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-1', newConstraint)).toThrow(
      ConstraintColumnNotUniqueError
    );
  });

  test('Primary Key를 구성하는 모든 컬럼은 NOT NULL 제약 조건이 있어야 한다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) => t.withId('table-1').withColumn((c) => c.withId('id-col')))
      )
      .build();

    const pkConstraint = createConstraintBuilder()
      .withName('pk_id')
      .withKind('PRIMARY_KEY')
      .withColumn((cc) => cc.withColumnId('id-col'))
      .build();

    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-1', pkConstraint)).not.toThrow();

    const table = db.schemas.find((s) => s.id === 'schema-1')?.tables.find((t) => t.id === 'table-1');
    expect(table?.constraints?.filter((c) => c.columns.some((cc) => cc.columnId === 'id-col'))?.length).toBe(2); // PK, NN
  });

  test('Unique 제약 조건은 Primary Key와 완전히 동일한 컬럼 조합을 가질 수 없다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) => t.withId('table-1').withColumn((c) => c.withId('id-col')))
      )
      .build();

    const pkConstraint = createConstraintBuilder()
      .withName('pk_id')
      .withKind('PRIMARY_KEY')
      .withColumn((cc) => cc.withColumnId('id-col'))
      .build();

    const ukConstraint = createConstraintBuilder()
      .withName('uk_id')
      .withKind('UNIQUE')
      .withColumn((cc) => cc.withColumnId('id-col'))
      .build();

    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-1', pkConstraint)).not.toThrow();
    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-1', ukConstraint)).toThrow(
      UniqueSameAsPrimaryKeyError
    );
  });

  test('동일한 컬럼 조합이지만 순서가 다른 제약조건은 서로 다른 것으로 인정되어야 한다.', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) =>
            t
              .withColumn((c) => c.withId('col1').withName('column1'))
              .withColumn((c) => c.withId('col2').withName('column2'))
          )
      )
      .build();

    const ukConstraint1 = createConstraintBuilder()
      .withName('uk_1_2')
      .withKind('UNIQUE')
      .withColumn((cc) => cc.withColumnId('col1').withSeqNo(1))
      .withColumn((cc) => cc.withColumnId('col2').withSeqNo(2))
      .build();

    const ukConstraint2 = createConstraintBuilder()
      .withName('uk_2_1')
      .withKind('UNIQUE')
      .withColumn((cc) => cc.withColumnId('col2').withSeqNo(1))
      .withColumn((cc) => cc.withColumnId('col1').withSeqNo(2))
      .build();

    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-1', ukConstraint1)).not.toThrow();
    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-1', ukConstraint2)).not.toThrow();
  });

  test('완전히 동일한 컬럼 조합과 순서를 가진 제약조건은 중복으로 간주되어야 한다.', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) =>
            t
              .withColumn((c) => c.withId('col1').withName('column1'))
              .withColumn((c) => c.withId('col2').withName('column2'))
          )
      )
      .build();

    const ukConstraint1 = createConstraintBuilder()
      .withName('uk_duplicate_1')
      .withKind('UNIQUE')
      .withColumn((cc) => cc.withColumnId('col1').withSeqNo(1))
      .withColumn((cc) => cc.withColumnId('col2').withSeqNo(2))
      .build();

    const ukConstraint2 = createConstraintBuilder()
      .withName('uk_duplicate_2')
      .withKind('UNIQUE')
      .withColumn((cc) => cc.withColumnId('col1').withSeqNo(1))
      .withColumn((cc) => cc.withColumnId('col2').withSeqNo(2))
      .build();

    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-1', ukConstraint1)).not.toThrow();
    expect(() => ERD_VALIDATOR.createConstraint(db, 'schema-1', 'table-1', ukConstraint2)).toThrow(
      DuplicateKeyDefinitionError
    );
  });

  test('존재하지 않는 제약조건 삭제 시 에러 발생', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) =>
          t
            .withId('table-1')
            .withName('posts')
            .withColumn((c) => c.withId('id-col').withName('id'))
            .withConstraint((c) =>
              c
                .withName('pk_posts')
                .withKind('PRIMARY_KEY')
                .withColumn((cc) => cc.withColumnId('id-col'))
            )
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.deleteConstraint(db, 'schema-1', 'table-1', 'non-existent-constraint-id')).toThrow(
      ConstraintNotExistError
    );
  });
});
