import { createIndexBuilder, createTestDatabase } from '../src/lib/builder';
import {
  IndexNameNotUniqueError,
  IndexTypeInvalidError,
  IndexColumnNotUniqueError,
  DuplicateIndexDefinitionError,
  IndexNotExistError,
  ERD_VALIDATOR,
  IndexColumnSortDirInvalidError,
} from '../src';

describe('Index validation', () => {
  test.skip('인덱스 이름은 테이블 내에서 중복될 수 없다', () => {
    const index = createIndexBuilder()
      .withName('idx_name')
      .withColumn((ic) => ic.withColumnId('name-col'))
      .build();
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('id-col').withName('id'))
            .withColumn((c) => c.withId('name-col').withName('name'))
            .withIndex((i) => i.withName('idx_name').withColumn((ic) => ic.withColumnId('id-col')))
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.createIndex(db, 'schema-1', 'table-1', index)).toThrow(IndexNameNotUniqueError);
  });

  test.skip('인덱스의 타입은 해당 데이터베이스 벤더에서 지원하는 유효한 값이어야 한다', () => {
    const index = createIndexBuilder()
      .withName('idx_invalid_type')
      .withColumn((ic) => ic.withColumnId('name-col'))
      .withType('GIN' as any) // MySQL에서 지원하지 않는 타입
      .build();
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('id-col').withName('id'))
            .withColumn((c) => c.withId('name-col').withName('name'))
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.createIndex(db, 'schema-1', 'table-1', index)).toThrow(IndexTypeInvalidError);
  });

  test.skip('하나의 인덱스에 동일한 컬럼이 중복으로 들어갈 수 없다 - createIndex', () => {
    const index = createIndexBuilder()
      .withName('idx_duplicate_column')
      .withColumn((ic) => ic.withColumnId('name-col'))
      .withColumn((ic) => ic.withColumnId('name-col')) // 중복 컬럼
      .build();

    const db = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) => t.withId('table-1').withColumn((c) => c.withId('name-col').withName('name')))
      )
      .build();

    expect(() => ERD_VALIDATOR.createIndex(db, 'schema-1', 'table-1', index)).toThrow(IndexColumnNotUniqueError);
  });

  test.skip('하나의 인덱스에 동일한 컬럼이 중복으로 들어갈 수 없다 - addColumnToIndex', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('column-1').withName('name'))
            .withIndex((i) => i.withName('idx_duplicate_column').withColumn((ic) => ic.withColumnId('column-1')))
        )
      )
      .build();

    expect(() =>
      ERD_VALIDATOR.addColumnToIndex(db, 'schema-1', 'table-1', 'index-1', {
        columnId: 'column-1',
        seqNo: 1,
        sortDir: 'ASC',
      })
    ).toThrow(IndexColumnNotUniqueError);
  });

  test.skip('인덱스 컬럼의 정렬 방향은 asc 또는 desc만 허용된다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) => t.withId('table-1').withColumn((c) => c.withId('id-col').withName('id')))
      )
      .build();

    const index = createIndexBuilder()
      .withName('idx_sort_dir')
      .withColumn((ic) => ic.withColumnId('id-col').withSortDir('INVALID_SORT' as any))
      .build();

    expect(() => ERD_VALIDATOR.createIndex(db, 'schema-1', 'table-1', index)).toThrow(IndexColumnSortDirInvalidError);
  });

  test.skip('컬럼 구성과 순서, 종류가 동일한 중복 인덱스를 생성할 수 없다', () => {
    // 동일한 인덱스는 조회 성능에 이점 없이 쓰기 성능 저하와 공간 낭비만 유발하므로 금지해야 한다.
    const newIndex = createIndexBuilder()
      .withName('idx_name_2')
      .withColumn((ic) => ic.withColumnId('name-col').withSeqNo(1))
      .withColumn((ic) => ic.withColumnId('email-col').withSeqNo(2))
      .build();

    const db = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('id-col').withName('id'))
            .withColumn((c) => c.withId('name-col').withName('name'))
            .withColumn((c) => c.withId('email-col').withName('email'))
            .withIndex((i) =>
              i
                .withName('idx_name_1')
                .withColumn((ic) => ic.withColumnId('name-col').withSeqNo(1))
                .withColumn((ic) => ic.withColumnId('email-col').withSeqNo(2))
            )
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.createIndex(db, 'schema-1', 'table-1', newIndex)).toThrow(DuplicateIndexDefinitionError);
  });

  test.skip('존재하지 않는 인덱스 삭제 시 에러 발생', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withName('users')
            .withColumn((c) => c.withId('id-col').withName('id').withDataType('INT'))
            .withColumn((c) => c.withId('email-col').withName('email').withDataType('VARCHAR').withLengthScale('100'))
            .withConstraint((c) => c.withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('id-col')))
            .withConstraint((c) => c.withKind('UNIQUE').withColumn((cc) => cc.withColumnId('email-col')))
            .withIndex((i) => i.withName('idx_email').withColumn((ic) => ic.withColumnId('email-col')))
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.deleteIndex(database, 'schema-1', 'table-1', 'non-existent')).toThrow(
      IndexNotExistError
    );
  });

  test.skip('인덱스 이름을 변경할 수 있다', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withName('users')
            .withColumn((c) => c.withId('email-col').withName('email').withDataType('VARCHAR').withLengthScale('100'))
            .withIndex((i) => i.withName('idx_email').withColumn((ic) => ic.withColumnId('email-col')))
            .withIndex((i) => i.withName('idx_name').withColumn((ic) => ic.withColumnId('name-col')))
        )
      )
      .build();

    expect(() =>
      ERD_VALIDATOR.changeIndexName(database, 'schema-1', 'table-1', 'idx_email', 'idx_user_email')
    ).not.toThrow();
  });

  test.skip('중복된 인덱스 이름으로 변경 시 에러 발생', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withName('users')
            .withColumn((c) => c.withId('email-col').withName('email').withDataType('VARCHAR').withLengthScale('100'))
            .withColumn((c) => c.withId('name-col').withName('name').withDataType('VARCHAR').withLengthScale('50'))
            .withIndex((i) =>
              i
                .withId('idx_email')
                .withName('idx_email')
                .withColumn((ic) => ic.withColumnId('email-col'))
            )
            .withIndex((i) =>
              i
                .withId('idx_name')
                .withName('idx_name')
                .withColumn((ic) => ic.withColumnId('name-col'))
            )
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.changeIndexName(database, 'schema-1', 'table-1', 'idx_email', 'idx_name')).toThrow(
      IndexNameNotUniqueError
    );
  });

  test.skip('존재하지 않는 인덱스 이름 변경 시 에러 발생', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withName('users')
            .withColumn((c) => c.withId('email-col').withName('email').withDataType('VARCHAR').withLengthScale('100'))
            .withColumn((c) => c.withId('name-col').withName('name').withDataType('VARCHAR').withLengthScale('50'))
            .withIndex((i) => i.withName('idx_email').withColumn((ic) => ic.withColumnId('email-col')))
            .withIndex((i) => i.withName('idx_name').withColumn((ic) => ic.withColumnId('name-col')))
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.changeIndexName(database, 'schema-1', 'table-1', 'non-existent', 'new_name')).toThrow(
      IndexNotExistError
    );
  });
});
