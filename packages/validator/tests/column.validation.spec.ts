import {
  createColumnBuilder,
  createRelationshipBuilder,
  createTestDatabase,
} from '../src/lib/builder';
import {
  ColumnDataTypeInvalidError,
  ColumnDataTypeRequiredError,
  ColumnLengthRequiredError,
  ColumnInvalidFormatError,
  ColumnNameIsReservedKeywordError,
  ColumnNameNotUniqueError,
  ColumnInvalidError,
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

  test.skip('validate 단계에서 데이터 타입이 빈 문자열이면 에러를 발생시킨다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) => t.withId('table-1').withColumn((c) => c.withId('id-col').withName('id2').withDataType('')))
      )
      .build();

    expect(() => ERD_VALIDATOR.validate(db)).toThrow(ColumnDataTypeRequiredError);
  });

  test.skip('validate 단계에서 데이터 타입이 null이면 에러를 발생시킨다', () => {
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
    ['', '빈 컬럼 이름'],
    ['a'.repeat(41), '너무 긴 컬럼 이름'],
  ])('컬럼의 이름은 1글자 이상 40글자 이하만 허용된다: %s', (name) => {
    const schemaId = 'schema-1';
    const tableId = 'table-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s.withId(schemaId).withTable((t) => t.withId(tableId).withColumn((c) => c.withId('id-col').withName('id2')))
      )
      .build();

    const column = createColumnBuilder().withName(name).build();

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).toThrow(ColumnInvalidError);
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

    expect(() => ERD_VALIDATOR.createColumn(baseDatabase, schemaId, tableId, column)).toThrow(ColumnInvalidFormatError);
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

  test('PK 타입 변경 시 FK 컬럼 타입이 함께 변경된다', () => {
    const schemaId = 'schema-1';
    const parentTableId = 'parent-table';
    const childTableId = 'child-table';
    const parentColumnId = 'parent-id';
    const fkColumnId = 'child-fk';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(parentTableId)
              .withName('parent')
              .withColumn((c) =>
                c.withId(parentColumnId).withName('id').withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_parent')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(parentColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(childTableId)
              .withName('child')
              .withColumn((c) =>
                c.withId(fkColumnId).withName('parent_id').withDataType('INT')
              )
          )
      )
      .build();

    const relationship = createRelationshipBuilder()
      .withId('rel-1')
      .withSrcTableId(childTableId)
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId(parentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(fkColumnId).withRefColumnId(parentColumnId)
      )
      .build();

    const databaseWithRelationship = ERD_VALIDATOR.createRelationship(
      baseDatabase,
      schemaId,
      relationship
    );

    const updatedDatabase = ERD_VALIDATOR.changeColumnType(
      databaseWithRelationship,
      schemaId,
      parentTableId,
      parentColumnId,
      'VARCHAR',
      '50'
    );

    const updatedParentColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === parentTableId)
      ?.columns.find((c) => c.id === parentColumnId);
    const updatedFkColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === childTableId)
      ?.columns.find((c) => c.id === fkColumnId);

    expect(updatedParentColumn?.dataType).toBe('VARCHAR');
    expect(updatedParentColumn?.lengthScale).toBe('50');
    expect(updatedFkColumn?.dataType).toBe('VARCHAR');
    expect(updatedFkColumn?.lengthScale).toBe('50');
  });

  test('PK 타입 변경 시 다단계 FK 컬럼 타입도 함께 변경된다', () => {
    const schemaId = 'schema-1';
    const grandparentTableId = 'grandparent-table';
    const parentTableId = 'parent-table';
    const childTableId = 'child-table';
    const grandparentColumnId = 'grandparent-id';
    const parentFkColumnId = 'parent-fk';
    const childFkColumnId = 'child-fk';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(grandparentTableId)
              .withName('grandparent')
              .withColumn((c) =>
                c
                  .withId(grandparentColumnId)
                  .withName('id')
                  .withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_grandparent')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(grandparentColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(parentTableId)
              .withName('parent')
              .withColumn((c) =>
                c
                  .withId(parentFkColumnId)
                  .withName('grandparent_id')
                  .withDataType('INT')
              )
          )
          .withTable((t) =>
            t
              .withId(childTableId)
              .withName('child')
              .withColumn((c) =>
                c
                  .withId(childFkColumnId)
                  .withName('grandparent_id')
                  .withDataType('INT')
              )
          )
      )
      .build();

    const parentRelationship = createRelationshipBuilder()
      .withId('rel-1')
      .withSrcTableId(parentTableId)
      .withName('fk_grandparent')
      .withKind('IDENTIFYING')
      .withTgtTableId(grandparentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(parentFkColumnId).withRefColumnId(grandparentColumnId)
      )
      .build();

    const dbWithParentRel = ERD_VALIDATOR.createRelationship(
      baseDatabase,
      schemaId,
      parentRelationship
    );

    const childRelationship = createRelationshipBuilder()
      .withId('rel-2')
      .withSrcTableId(childTableId)
      .withName('fk_parent')
      .withKind('IDENTIFYING')
      .withTgtTableId(parentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(childFkColumnId).withRefColumnId(parentFkColumnId)
      )
      .build();

    const dbWithBothRels = ERD_VALIDATOR.createRelationship(
      dbWithParentRel,
      schemaId,
      childRelationship
    );

    const updatedDatabase = ERD_VALIDATOR.changeColumnType(
      dbWithBothRels,
      schemaId,
      grandparentTableId,
      grandparentColumnId,
      'BIGINT'
    );

    const updatedGrandparentColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === grandparentTableId)
      ?.columns.find((c) => c.id === grandparentColumnId);
    const updatedParentFkColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === parentTableId)
      ?.columns.find((c) => c.id === parentFkColumnId);
    const updatedChildFkColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === childTableId)
      ?.columns.find((c) => c.id === childFkColumnId);

    expect(updatedGrandparentColumn?.dataType).toBe('BIGINT');
    expect(updatedParentFkColumn?.dataType).toBe('BIGINT');
    expect(updatedChildFkColumn?.dataType).toBe('BIGINT');
  });

  test('중간 FK 컬럼 타입 변경 시 상위/하위 컬럼 타입도 함께 변경된다', () => {
    const schemaId = 'schema-1';
    const grandparentTableId = 'grandparent-table';
    const parentTableId = 'parent-table';
    const childTableId = 'child-table';
    const grandparentColumnId = 'grandparent-id';
    const parentFkColumnId = 'parent-fk';
    const childFkColumnId = 'child-fk';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(grandparentTableId)
              .withName('grandparent')
              .withColumn((c) =>
                c
                  .withId(grandparentColumnId)
                  .withName('id')
                  .withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_grandparent')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(grandparentColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(parentTableId)
              .withName('parent')
              .withColumn((c) =>
                c
                  .withId(parentFkColumnId)
                  .withName('grandparent_id')
                  .withDataType('INT')
              )
          )
          .withTable((t) =>
            t
              .withId(childTableId)
              .withName('child')
              .withColumn((c) =>
                c
                  .withId(childFkColumnId)
                  .withName('grandparent_id')
                  .withDataType('INT')
              )
          )
      )
      .build();

    const parentRelationship = createRelationshipBuilder()
      .withId('rel-1')
      .withSrcTableId(parentTableId)
      .withName('fk_grandparent')
      .withKind('IDENTIFYING')
      .withTgtTableId(grandparentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(parentFkColumnId).withRefColumnId(grandparentColumnId)
      )
      .build();

    const dbWithParentRel = ERD_VALIDATOR.createRelationship(
      baseDatabase,
      schemaId,
      parentRelationship
    );

    const childRelationship = createRelationshipBuilder()
      .withId('rel-2')
      .withSrcTableId(childTableId)
      .withName('fk_parent')
      .withKind('IDENTIFYING')
      .withTgtTableId(parentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(childFkColumnId).withRefColumnId(parentFkColumnId)
      )
      .build();

    const dbWithBothRels = ERD_VALIDATOR.createRelationship(
      dbWithParentRel,
      schemaId,
      childRelationship
    );

    const updatedDatabase = ERD_VALIDATOR.changeColumnType(
      dbWithBothRels,
      schemaId,
      parentTableId,
      parentFkColumnId,
      'BIGINT'
    );

    const updatedGrandparentColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === grandparentTableId)
      ?.columns.find((c) => c.id === grandparentColumnId);
    const updatedParentFkColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === parentTableId)
      ?.columns.find((c) => c.id === parentFkColumnId);
    const updatedChildFkColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === childTableId)
      ?.columns.find((c) => c.id === childFkColumnId);

    // 상위(grandparent), 자신(parent), 하위(child) 모두 변경되어야 함
    expect(updatedGrandparentColumn?.dataType).toBe('BIGINT');
    expect(updatedParentFkColumn?.dataType).toBe('BIGINT');
    expect(updatedChildFkColumn?.dataType).toBe('BIGINT');
  });

  test('최하위 FK 컬럼 타입 변경 시 상위 컬럼 타입도 함께 변경된다', () => {
    const schemaId = 'schema-1';
    const grandparentTableId = 'grandparent-table';
    const parentTableId = 'parent-table';
    const childTableId = 'child-table';
    const grandparentColumnId = 'grandparent-id';
    const parentFkColumnId = 'parent-fk';
    const childFkColumnId = 'child-fk';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(grandparentTableId)
              .withName('grandparent')
              .withColumn((c) =>
                c
                  .withId(grandparentColumnId)
                  .withName('id')
                  .withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_grandparent')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(grandparentColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(parentTableId)
              .withName('parent')
              .withColumn((c) =>
                c
                  .withId(parentFkColumnId)
                  .withName('grandparent_id')
                  .withDataType('INT')
              )
          )
          .withTable((t) =>
            t
              .withId(childTableId)
              .withName('child')
              .withColumn((c) =>
                c
                  .withId(childFkColumnId)
                  .withName('grandparent_id')
                  .withDataType('INT')
              )
          )
      )
      .build();

    const parentRelationship = createRelationshipBuilder()
      .withId('rel-1')
      .withSrcTableId(parentTableId)
      .withName('fk_grandparent')
      .withKind('IDENTIFYING')
      .withTgtTableId(grandparentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(parentFkColumnId).withRefColumnId(grandparentColumnId)
      )
      .build();

    const dbWithParentRel = ERD_VALIDATOR.createRelationship(
      baseDatabase,
      schemaId,
      parentRelationship
    );

    const childRelationship = createRelationshipBuilder()
      .withId('rel-2')
      .withSrcTableId(childTableId)
      .withName('fk_parent')
      .withKind('IDENTIFYING')
      .withTgtTableId(parentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(childFkColumnId).withRefColumnId(parentFkColumnId)
      )
      .build();

    const dbWithBothRels = ERD_VALIDATOR.createRelationship(
      dbWithParentRel,
      schemaId,
      childRelationship
    );

    // 최하위 FK 컬럼(childFkColumnId) 타입 변경
    const updatedDatabase = ERD_VALIDATOR.changeColumnType(
      dbWithBothRels,
      schemaId,
      childTableId,
      childFkColumnId,
      'BIGINT'
    );

    const updatedGrandparentColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === grandparentTableId)
      ?.columns.find((c) => c.id === grandparentColumnId);
    const updatedParentFkColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === parentTableId)
      ?.columns.find((c) => c.id === parentFkColumnId);
    const updatedChildFkColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === childTableId)
      ?.columns.find((c) => c.id === childFkColumnId);

    // 상위(grandparent, parent) 모두 변경되어야 함
    expect(updatedGrandparentColumn?.dataType).toBe('BIGINT');
    expect(updatedParentFkColumn?.dataType).toBe('BIGINT');
    expect(updatedChildFkColumn?.dataType).toBe('BIGINT');
  });

  test('4단계 체인에서 중간 컬럼 타입 변경 시 전체 체인이 변경된다', () => {
    const schemaId = 'schema-1';
    const greatGrandparentTableId = 'great-grandparent-table';
    const grandparentTableId = 'grandparent-table';
    const parentTableId = 'parent-table';
    const childTableId = 'child-table';
    const greatGrandparentColumnId = 'great-grandparent-id';
    const grandparentFkColumnId = 'grandparent-fk';
    const parentFkColumnId = 'parent-fk';
    const childFkColumnId = 'child-fk';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(greatGrandparentTableId)
              .withName('great_grandparent')
              .withColumn((c) =>
                c
                  .withId(greatGrandparentColumnId)
                  .withName('id')
                  .withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_great_grandparent')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(greatGrandparentColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(grandparentTableId)
              .withName('grandparent')
              .withColumn((c) =>
                c
                  .withId(grandparentFkColumnId)
                  .withName('great_grandparent_id')
                  .withDataType('INT')
              )
          )
          .withTable((t) =>
            t
              .withId(parentTableId)
              .withName('parent')
              .withColumn((c) =>
                c
                  .withId(parentFkColumnId)
                  .withName('grandparent_id')
                  .withDataType('INT')
              )
          )
          .withTable((t) =>
            t
              .withId(childTableId)
              .withName('child')
              .withColumn((c) =>
                c
                  .withId(childFkColumnId)
                  .withName('parent_id')
                  .withDataType('INT')
              )
          )
      )
      .build();

    // 관계 생성: great-grandparent → grandparent
    const rel1 = createRelationshipBuilder()
      .withId('rel-1')
      .withSrcTableId(grandparentTableId)
      .withName('fk_great_grandparent')
      .withKind('IDENTIFYING')
      .withTgtTableId(greatGrandparentTableId)
      .withColumn((rc) =>
        rc
          .withFkColumnId(grandparentFkColumnId)
          .withRefColumnId(greatGrandparentColumnId)
      )
      .build();

    const db1 = ERD_VALIDATOR.createRelationship(baseDatabase, schemaId, rel1);

    // 관계 생성: grandparent → parent
    const rel2 = createRelationshipBuilder()
      .withId('rel-2')
      .withSrcTableId(parentTableId)
      .withName('fk_grandparent')
      .withKind('IDENTIFYING')
      .withTgtTableId(grandparentTableId)
      .withColumn((rc) =>
        rc
          .withFkColumnId(parentFkColumnId)
          .withRefColumnId(grandparentFkColumnId)
      )
      .build();

    const db2 = ERD_VALIDATOR.createRelationship(db1, schemaId, rel2);

    // 관계 생성: parent → child
    const rel3 = createRelationshipBuilder()
      .withId('rel-3')
      .withSrcTableId(childTableId)
      .withName('fk_parent')
      .withKind('IDENTIFYING')
      .withTgtTableId(parentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(childFkColumnId).withRefColumnId(parentFkColumnId)
      )
      .build();

    const db3 = ERD_VALIDATOR.createRelationship(db2, schemaId, rel3);

    // 중간(grandparent) 컬럼 타입 변경
    const updatedDatabase = ERD_VALIDATOR.changeColumnType(
      db3,
      schemaId,
      grandparentTableId,
      grandparentFkColumnId,
      'BIGINT'
    );

    const updatedGreatGrandparentColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === greatGrandparentTableId)
      ?.columns.find((c) => c.id === greatGrandparentColumnId);
    const updatedGrandparentColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === grandparentTableId)
      ?.columns.find((c) => c.id === grandparentFkColumnId);
    const updatedParentColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === parentTableId)
      ?.columns.find((c) => c.id === parentFkColumnId);
    const updatedChildColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === childTableId)
      ?.columns.find((c) => c.id === childFkColumnId);

    // 4단계 체인 전체가 변경되어야 함
    expect(updatedGreatGrandparentColumn?.dataType).toBe('BIGINT');
    expect(updatedGrandparentColumn?.dataType).toBe('BIGINT');
    expect(updatedParentColumn?.dataType).toBe('BIGINT');
    expect(updatedChildColumn?.dataType).toBe('BIGINT');
  });

  test('분기 구조에서 한 분기 컬럼 변경 시 다른 분기도 변경된다', () => {
    const schemaId = 'schema-1';
    const rootTableId = 'root-table';
    const branch1TableId = 'branch1-table';
    const branch2TableId = 'branch2-table';
    const rootColumnId = 'root-id';
    const branch1FkColumnId = 'branch1-fk';
    const branch2FkColumnId = 'branch2-fk';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(rootTableId)
              .withName('root')
              .withColumn((c) =>
                c.withId(rootColumnId).withName('id').withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_root')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(rootColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(branch1TableId)
              .withName('branch1')
              .withColumn((c) =>
                c
                  .withId(branch1FkColumnId)
                  .withName('root_id')
                  .withDataType('INT')
              )
          )
          .withTable((t) =>
            t
              .withId(branch2TableId)
              .withName('branch2')
              .withColumn((c) =>
                c
                  .withId(branch2FkColumnId)
                  .withName('root_id')
                  .withDataType('INT')
              )
          )
      )
      .build();

    // 관계 생성: root → branch1
    const rel1 = createRelationshipBuilder()
      .withId('rel-1')
      .withSrcTableId(branch1TableId)
      .withName('fk_root_1')
      .withKind('IDENTIFYING')
      .withTgtTableId(rootTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(branch1FkColumnId).withRefColumnId(rootColumnId)
      )
      .build();

    const db1 = ERD_VALIDATOR.createRelationship(baseDatabase, schemaId, rel1);

    // 관계 생성: root → branch2
    const rel2 = createRelationshipBuilder()
      .withId('rel-2')
      .withSrcTableId(branch2TableId)
      .withName('fk_root_2')
      .withKind('IDENTIFYING')
      .withTgtTableId(rootTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(branch2FkColumnId).withRefColumnId(rootColumnId)
      )
      .build();

    const db2 = ERD_VALIDATOR.createRelationship(db1, schemaId, rel2);

    // branch1의 FK 컬럼 타입 변경
    const updatedDatabase = ERD_VALIDATOR.changeColumnType(
      db2,
      schemaId,
      branch1TableId,
      branch1FkColumnId,
      'BIGINT'
    );

    const updatedRootColumn = updatedDatabase.schemas[0].tables
      .find((t) => t.id === rootTableId)
      ?.columns.find((c) => c.id === rootColumnId);
    const updatedBranch1Column = updatedDatabase.schemas[0].tables
      .find((t) => t.id === branch1TableId)
      ?.columns.find((c) => c.id === branch1FkColumnId);
    const updatedBranch2Column = updatedDatabase.schemas[0].tables
      .find((t) => t.id === branch2TableId)
      ?.columns.find((c) => c.id === branch2FkColumnId);

    // root, branch1, branch2 모두 변경되어야 함
    expect(updatedRootColumn?.dataType).toBe('BIGINT');
    expect(updatedBranch1Column?.dataType).toBe('BIGINT');
    expect(updatedBranch2Column?.dataType).toBe('BIGINT');
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
});
