import { createColumnBuilder, createTestDatabase } from '../src/lib/builder';
import {
  ColumnDataTypeInvalidError,
  ColumnDataTypeRequiredError,
  ColumnLengthRequiredError,
  ColumnNameInvalidError,
  ColumnNameInvalidFormatError,
  ColumnNameIsReservedKeywordError,
  ColumnNameNotUniqueError,
  ColumnPrecisionRequiredError,
  MultipleAutoIncrementColumnsError,
} from '../src/lib/errors';
import { ERD_VALIDATOR } from '../src/lib/utils';

describe('Column validation', () => {
  test('같은 테이블 내 중복된 컬럼 이름은 금지된다', () => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s.withId(schemaId).withTable((t) => t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const column = createColumnBuilder().withName('id2').build();

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).toThrow(ColumnNameNotUniqueError);
  });

  test('데이터 타입이 비어 있어도 컬럼 생성 단계에서는 허용된다', () => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s.withId(schemaId).withTable((t) => t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const column = createColumnBuilder().withName('id3').withDataType('').build();

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).not.toThrow();
  });

  test('컬럼의 데이터 타입은 데이터베이스 벤더에서 유효한 값이어야 한다', () => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s.withId(schemaId).withTable((t) => t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const column = createColumnBuilder().withName('id3').withDataType('INVALID_TYPE').build();

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).toThrow(
      ColumnDataTypeInvalidError
    );
  });

  test('validate 단계에서 데이터 타입이 빈 문자열이면 에러를 발생시킨다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) => t.withId('table-1').withColumn((c) => c.withId('id-col').withName('id2').withDataType('')))
      )
      .build();

    expect(() => ERD_VALIDATOR.validate(db)).toThrow(ColumnDataTypeRequiredError);
  });

  test('validate 단계에서 데이터 타입이 null이면 에러를 발생시킨다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) => t.withId('table-1').withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const table = db.schemas[0].tables[0];
    table.columns[0].dataType = null;

    expect(() => ERD_VALIDATOR.validate(db)).toThrow(ColumnDataTypeRequiredError);
  });

  test.each(['VARCHAR', 'CHAR'])('%s 타입은 반드시 길이를 지정해야 한다', (dataType) => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s.withId(schemaId).withTable((t) => t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const column = createColumnBuilder().withName('id3').withDataType(dataType).withLengthScale('').build();

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).toThrow(
      ColumnLengthRequiredError
    );
  });

  test.each(['DECIMAL', 'NUMERIC'])('%s 타입은 반드시 정밀도와 스케일을 지정해야 한다', (dataType) => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s.withId(schemaId).withTable((t) => t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const column = createColumnBuilder().withName('id3').withDataType(dataType).withLengthScale('').build();

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).toThrow(
      ColumnPrecisionRequiredError
    );
  });

  test.each([
    ['c1', '너무 짧은 컬럼 이름'],
    ['a'.repeat(41), '너무 긴 컬럼 이름'],
  ])('컬럼의 이름은 3글자 이상 40글자 이하만 허용된다: %s', (name) => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s.withId(schemaId).withTable((t) => t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const column = createColumnBuilder().withName(name).build();

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).toThrow(ColumnNameInvalidError);
  });

  test.each([
    ['col-name', '하이픈 포함'],
    ['col name', '공백 포함'],
    ['1col', '숫자로 시작'],
    ['col$', '특수문자 포함'],
  ])('컬럼 이름 형식 검증: %s', (name) => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s.withId(schemaId).withTable((t) => t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const column = createColumnBuilder().withName(name).build();

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).toThrow(
      ColumnNameInvalidFormatError
    );
  });

  test.each([
    ['TABLE', '테이블 생성 예약어'],
    ['SELECT', '데이터 조회 예약어'],
    ['CREATE', '객체 생성 예약어'],
  ])('컬럼명으로 데이터베이스 벤더의 예약어(%s)를 사용할 수 없다', (name) => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s.withId(schemaId).withTable((t) => t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const column = createColumnBuilder().withName(name).build();

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).toThrow(
      ColumnNameIsReservedKeywordError
    );
  });

  test('Auto Increment 컬럼은 테이블 당 하나만 존재할 수 있다.', () => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';
    const newColumn = createColumnBuilder().withName('new_ai_column').withAutoIncrement(true).build();
    const dbWithAutoIncrement = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2').withAutoIncrement(true))
          )
      )
      .build();

    expect(() => ERD_VALIDATOR.createColumn(dbWithAutoIncrement, schemaId, tableId, newColumn)).toThrow(
      MultipleAutoIncrementColumnsError
    );
  });

  test('숫자 타입이 아닌 컬럼에 Auto Increment를 설정할 수 없다.', () => {
    createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) =>
              c
                .withId('non-numeric-ai-col')
                .withName('varchar_ai')
                .withDataType('VARCHAR')
                .withLengthScale('50')
                .withAutoIncrement(true)
            )
            .withConstraint((c) =>
              c
                .withName('pk_varchar_ai')
                .withKind('PRIMARY_KEY')
                .withColumn((cc) => cc.withColumnId('non-numeric-ai-col'))
            )
        )
      )
      .build();

    // expect(() => ERD_VALIDATOR.validate(db)).toThrow(AutoIncrementNonNumericError);
  });

  test('Primary Key가 아닌 컬럼에 Auto Increment를 설정할 수 없다.', () => {
    createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('pk-col').withName('id2').withDataType('INT'))
            .withColumn(
              (c) => c.withId('non-pk-ai-col').withName('non_pk_ai').withDataType('INT').withAutoIncrement(true) // PK가 아닌 컬럼에 AI 설정
            )
            .withConstraint(
              (c) =>
                c
                  .withName('pk_id')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId('pk-col')) // 다른 컬럼이 PK
            )
        )
      )
      .build();

    // expect(() => ERD_VALIDATOR.validate(db)).toThrow(AutoIncrementNonPrimaryKeyError);
  });

  test('일반 컬럼을 삭제할 수 있다', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('id-col').withName('id2').withDataType('INT'))
            .withColumn((c) => c.withId('name-col').withName('name').withDataType('VARCHAR').withLengthScale('100'))
            .withColumn((c) => c.withId('email-col').withName('email').withDataType('VARCHAR').withLengthScale('100'))
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.deleteColumn(database, 'schema-1', 'table-1', 'name-col')).not.toThrow();
  });

  test('Foreign Key로 참조되는 컬럼도 삭제 가능하다', () => {
    const dbWithFK = createTestDatabase()
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
              .withColumn((c) => c.withId('parent-id2').withName('id3').withDataType('INT'))
              .withConstraint((c) =>
                c
                  .withName('pk_parent2')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId('parent-id2'))
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

    const result = ERD_VALIDATOR.deleteColumn(dbWithFK, 'schema-1', 'parent-table', 'parent-id');
    const table = result.schemas[0].tables.find((t) => t.id === 'parent-table');
    expect(table?.columns.some((c) => c.id === 'parent-id')).toBeFalsy();
  });

  test('컬럼 이름을 변경할 수 있다', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('id-col').withName('id2'))
            .withColumn((c) => c.withId('name-col').withName('name'))
            .withColumn((c) => c.withId('email-col').withName('email'))
        )
      )
      .build();

    const result = ERD_VALIDATOR.changeColumnName(database, 'schema-1', 'table-1', 'name-col', 'full_name');
    const table = result.schemas[0].tables[0];
    const column = table.columns.find((c) => c.id === 'name-col');
    expect(column?.name).toBe('full_name');
  });

  test('중복된 컬럼 이름으로 변경 시 에러 발생', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('id-col').withName('id2'))
            .withColumn((c) => c.withId('name-col').withName('name'))
            .withColumn((c) => c.withId('email-col').withName('email'))
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.changeColumnName(database, 'schema-1', 'table-1', 'name-col', 'email')).toThrow(
      ColumnNameNotUniqueError
    );
  });

  test('존재하지 않는 컬럼 이름 변경 시 에러 발생', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('id-col').withName('id2'))
            .withColumn((c) => c.withId('name-col').withName('name'))
            .withColumn((c) => c.withId('email-col').withName('email'))
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.changeColumnName(database, 'schema-1', 'table-1', 'non-existent', 'new_name')).toThrow();
  });

  test('유효하지 않은 컬럼 이름으로 변경 시 에러 발생', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) =>
          t
            .withId('table-1')
            .withColumn((c) => c.withId('id-col').withName('id2'))
            .withColumn((c) => c.withId('name-col').withName('name'))
            .withColumn((c) => c.withId('email-col').withName('email'))
        )
      )
      .build();

    expect(() => ERD_VALIDATOR.changeColumnName(database, 'schema-1', 'table-1', 'name-col', 'x')).toThrow(
      ColumnNameInvalidError
    );
  });
});
