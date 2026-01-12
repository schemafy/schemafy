import { createRelationshipBuilder, createTestDatabase } from '../src/lib/builder';
import {
  RelationshipEmptyError,
  RelationshipNameNotUniqueError,
  RelationshipTargetTableNotExistError,
  RelationshipCyclicReferenceError,
} from '../src/lib/errors';
import { ERD_VALIDATOR } from '../src/lib/utils';
import type { Table, Constraint } from '../src/lib/types';

describe('Relationship validation', () => {
  test('관계는 반드시 하나 이상의 컬럼 매핑을 가져야 한다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) =>
            t
              .withId('parent-table')
              .withName('parent')
              .withColumn((c) => c.withId('id-col').withName('id2'))
          )
          .withTable((t) => t.withId('child-table').withName('child'))
      )
      .build();

    // createRelationship을 하는 경우에, 외래키 컬럼은 자동으로 생성됨. withPkColumnId만 지정하면 됨. (가정)
    const relationship = createRelationshipBuilder()
      .withFkTableId('child-table')
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withPkTableId('parent-table')
      .build();

    expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', relationship)).toThrow(RelationshipEmptyError);
  });

  test('테이블이 자기 자신을 참조하는 관계를 맺을 수 있다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) =>
          t
            .withId('employee-table')
            .withName('employee')
            .withColumn((c) => c.withId('id-col').withName('id2'))
            .withColumn((c) => c.withId('manager-col').withName('manager_id'))
            .withConstraint((c) =>
              c
                .withName('pk_employee')
                .withKind('PRIMARY_KEY')
                .withColumn((cc) => cc.withColumnId('id-col'))
            )
        )
      )
      .build();

    const relationship = createRelationshipBuilder()
      .withFkTableId('employee-table')
      .withName('fk_employee')
      .withKind('NON_IDENTIFYING')
      .withPkTableId('employee-table')
      .withColumn((rc) => rc.withPkColumnId('id-col'))
      .build();

    // TODO: 이 경우에는 참조하는 테이블이 자기 자신이므로 컬럼을 추가하고 릴레이션을 추가하는데, 내부적으로 중복이 생기면 안된다.
    expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', relationship)).not.toThrow();
  });

  test('기존 테이블에 중복된 이름의 관계를 추가할 수 없다.', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) =>
            t
              .withId('parent-table')
              .withName('parent')
              .withColumn((c) => c.withId('parent-id').withName('id2'))
          )
          .withTable((t) =>
            t
              .withId('child-table')
              .withName('child')
              .withColumn((c) => c.withId('child-id').withName('id2'))
          )
      )
      .build();

    const relationship = createRelationshipBuilder()
      .withFkTableId('child-table')
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withPkTableId('parent-table')
      .withColumn((rc) => rc.withPkColumnId('parent-id'))
      .build();

    let duplicateDatabase = database;

    expect(
      () => (duplicateDatabase = ERD_VALIDATOR.createRelationship(database, 'schema-1', relationship))
    ).not.toThrow();

    const duplicateRelationship = createRelationshipBuilder()
      .withFkTableId('child-table')
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withPkTableId('parent-table')
      .withColumn((rc) => rc.withPkColumnId('parent-id'))
      .build();

    expect(() => ERD_VALIDATOR.createRelationship(duplicateDatabase, 'schema-1', duplicateRelationship)).toThrow(
      RelationshipNameNotUniqueError
    );
  });

  test('관계 종류를 IDENTIFYING으로 변경하면 FK 컬럼이 PK에 포함된다', () => {
    const schemaId = 'schema-1';
    const parentTableId = 'parent-table';
    const childTableId = 'child-table';
    const parentColumnId = 'parent-id';
    const childPkColumnId = 'child-id';
    const fkColumnId = 'child-fk';
    const relationshipId = 'rel-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(parentTableId)
              .withName('parent')
              .withColumn((c) =>
                c.withId(parentColumnId).withName('id2').withDataType('INT')
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
                c.withId(childPkColumnId).withName('id2').withDataType('INT')
              )
              .withColumn((c) =>
                c.withId(fkColumnId).withName('parent_id').withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_child')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(childPkColumnId))
              )
          )
      )
      .build();

    const relationship = createRelationshipBuilder()
      .withId(relationshipId)
      .withFkTableId(childTableId)
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withPkTableId(parentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(fkColumnId).withPkColumnId(parentColumnId)
      )
      .build();

    const databaseWithRelationship = ERD_VALIDATOR.createRelationship(
      baseDatabase,
      schemaId,
      relationship
    );

    const updatedDatabase = ERD_VALIDATOR.changeRelationshipKind(
      databaseWithRelationship,
      schemaId,
      relationshipId,
      'IDENTIFYING'
    );

    const childTable = updatedDatabase.schemas[0].tables.find(
      (t) => t.id === childTableId
    );
    const updatedRelationship = childTable?.relationships.find(
      (r) => r.id === relationshipId
    );
    const childPk = childTable?.constraints.find(
      (c) => c.kind === 'PRIMARY_KEY'
    );
    const pkColumnIds = childPk?.columns.map((cc) => cc.columnId) ?? [];

    expect(updatedRelationship?.kind).toBe('IDENTIFYING');
    expect(pkColumnIds).toContain(fkColumnId);
  });

  test('관계 종류를 NON_IDENTIFYING으로 변경하면 FK 컬럼이 PK에서 제거된다', () => {
    const schemaId = 'schema-1';
    const parentTableId = 'parent-table';
    const childTableId = 'child-table';
    const parentColumnId = 'parent-id';
    const childPkColumnId = 'child-id';
    const fkColumnId = 'child-fk';
    const relationshipId = 'rel-1';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(parentTableId)
              .withName('parent')
              .withColumn((c) =>
                c.withId(parentColumnId).withName('id2').withDataType('INT')
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
                c.withId(childPkColumnId).withName('id2').withDataType('INT')
              )
              .withColumn((c) =>
                c.withId(fkColumnId).withName('parent_id').withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_child')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(childPkColumnId))
              )
          )
      )
      .build();

    const relationship = createRelationshipBuilder()
      .withId(relationshipId)
      .withFkTableId(childTableId)
      .withName('fk_parent')
      .withKind('IDENTIFYING')
      .withPkTableId(parentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(fkColumnId).withPkColumnId(parentColumnId)
      )
      .build();

    const databaseWithRelationship = ERD_VALIDATOR.createRelationship(
      baseDatabase,
      schemaId,
      relationship
    );

    const updatedDatabase = ERD_VALIDATOR.changeRelationshipKind(
      databaseWithRelationship,
      schemaId,
      relationshipId,
      'NON_IDENTIFYING'
    );

    const childTable = updatedDatabase.schemas[0].tables.find(
      (t) => t.id === childTableId
    );
    const updatedRelationship = childTable?.relationships.find(
      (r) => r.id === relationshipId
    );
    const childPk = childTable?.constraints.find(
      (c) => c.kind === 'PRIMARY_KEY'
    );
    const pkColumnIds = childPk?.columns.map((cc) => cc.columnId) ?? [];

    expect(updatedRelationship?.kind).toBe('NON_IDENTIFYING');
    expect(pkColumnIds).not.toContain(fkColumnId);
  });

  test('관계 종류 변경 시 하위 관계가 삭제되지 않는다', () => {
    const schemaId = 'schema-1';
    const parentTableId = 'parent-table';
    const childTableId = 'child-table';
    const grandchildTableId = 'grandchild-table';
    const parentPkColumnId = 'parent-id';
    const childPkColumnId = 'child-id';
    const childFkColumnId = 'child-parent-fk';
    const grandchildPkColumnId = 'grandchild-id';
    const grandchildFkColumnId = 'grandchild-child-fk';
    const parentChildRelationshipId = 'rel-parent-child';
    const grandchildRelationshipId = 'rel-grandchild-child';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(parentTableId)
              .withName('parent')
              .withColumn((c) =>
                c.withId(parentPkColumnId).withName('id2').withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_parent')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(parentPkColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(childTableId)
              .withName('child')
              .withColumn((c) =>
                c.withId(childPkColumnId).withName('id2').withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_child')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(childPkColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(grandchildTableId)
              .withName('grandchild')
              .withColumn((c) =>
                c
                  .withId(grandchildPkColumnId)
                  .withName('id2')
                  .withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_grandchild')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(grandchildPkColumnId))
              )
          )
      )
      .build();

    const parentChildRelationship = createRelationshipBuilder()
      .withId(parentChildRelationshipId)
      .withFkTableId(childTableId)
      .withName('fk_parent')
      .withKind('IDENTIFYING')
      .withPkTableId(parentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(childFkColumnId).withPkColumnId(parentPkColumnId)
      )
      .build();

    const databaseWithParentRelationship = ERD_VALIDATOR.createRelationship(
      baseDatabase,
      schemaId,
      parentChildRelationship
    );

    const grandchildRelationship = createRelationshipBuilder()
      .withId(grandchildRelationshipId)
      .withFkTableId(grandchildTableId)
      .withName('fk_child_parent')
      .withKind('NON_IDENTIFYING')
      .withPkTableId(childTableId)
      .withColumn((rc) =>
        rc
          .withFkColumnId(grandchildFkColumnId)
          .withPkColumnId(childFkColumnId)
      )
      .build();

    const databaseWithGrandchildRelationship = ERD_VALIDATOR.createRelationship(
      databaseWithParentRelationship,
      schemaId,
      grandchildRelationship
    );

    const updatedDatabase = ERD_VALIDATOR.changeRelationshipKind(
      databaseWithGrandchildRelationship,
      schemaId,
      parentChildRelationshipId,
      'NON_IDENTIFYING'
    );

    const updatedChildTable = updatedDatabase.schemas[0].tables.find(
      (t) => t.id === childTableId
    );
    const updatedGrandchildTable = updatedDatabase.schemas[0].tables.find(
      (t) => t.id === grandchildTableId
    );
    const remainingRelationship = updatedGrandchildTable?.relationships.find(
      (r) => r.id === grandchildRelationshipId
    );

    expect(updatedChildTable?.columns.some((c) => c.id === childFkColumnId)).toBe(
      true
    );
    expect(
      updatedGrandchildTable?.columns.some((c) => c.id === grandchildFkColumnId)
    ).toBe(true);
    expect(remainingRelationship).toBeDefined();
    expect(remainingRelationship?.kind).toBe('NON_IDENTIFYING');
  });

  test('IDENTIFYING 관계가 부모 PK 제거 시 자동으로 NON_IDENTIFYING으로 변환된다', () => {
    const schemaId = 'schema-1';
    const parentTableId = 'parent-table';
    const childTableId = 'child-table';
    const grandchildTableId = 'grandchild-table';
    const parentPkColumnId = 'parent-id';
    const childPkColumnId = 'child-id';
    const grandchildPkColumnId = 'grandchild-id';
    const parentChildRelationshipId = 'rel-parent-child';
    const childGrandchildRelationshipId = 'rel-child-grandchild';

    const baseDatabase = createTestDatabase()
      .withSchema((s) =>
        s
          .withId(schemaId)
          .withTable((t) =>
            t
              .withId(parentTableId)
              .withName('parent')
              .withColumn((c) =>
                c.withId(parentPkColumnId).withName('id').withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_parent')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(parentPkColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(childTableId)
              .withName('child')
              .withColumn((c) =>
                c.withId(childPkColumnId).withName('id').withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_child')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(childPkColumnId))
              )
          )
          .withTable((t) =>
            t
              .withId(grandchildTableId)
              .withName('grandchild')
              .withColumn((c) =>
                c
                  .withId(grandchildPkColumnId)
                  .withName('id')
                  .withDataType('INT')
              )
              .withConstraint((c) =>
                c
                  .withName('pk_grandchild')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId(grandchildPkColumnId))
              )
          )
      )
      .build();

    const parentChildRelationship = createRelationshipBuilder()
      .withId(parentChildRelationshipId)
      .withFkTableId(childTableId)
      .withName('fk_parent_child')
      .withKind('IDENTIFYING')
      .withPkTableId(parentTableId)
      .withColumn((rc) => rc.withPkColumnId(parentPkColumnId))
      .build();

    let database = ERD_VALIDATOR.createRelationship(
      baseDatabase,
      schemaId,
      parentChildRelationship
    );

    let childTable = database.schemas[0].tables.find((t) => t.id === childTableId);
    const childFkColumnId = childTable?.columns.find(
      (c) => c.name === 'parent_id'
    )?.id;

    const childGrandchildRelationship = createRelationshipBuilder()
      .withId(childGrandchildRelationshipId)
      .withFkTableId(grandchildTableId)
      .withName('fk_child_grandchild')
      .withKind('IDENTIFYING')
      .withPkTableId(childTableId)
      .withColumn((rc) => rc.withPkColumnId(childFkColumnId!))
      .build();

    database = ERD_VALIDATOR.createRelationship(
      database,
      schemaId,
      childGrandchildRelationship
    );

    childTable = database.schemas[0].tables.find((t) => t.id === childTableId);
    const initialChildRel = childTable?.relationships.find(
      (r) => r.id === parentChildRelationshipId
    );

    const grandchildTable = database.schemas[0].tables.find(
      (t) => t.id === grandchildTableId
    );
    const initialGrandchildRel = grandchildTable?.relationships.find(
      (r) => r.id === childGrandchildRelationshipId
    );

    expect(initialChildRel?.kind).toBe('IDENTIFYING');
    expect(initialGrandchildRel?.kind).toBe('IDENTIFYING');

    database = ERD_VALIDATOR.changeRelationshipKind(
      database,
      schemaId,
      parentChildRelationshipId,
      'NON_IDENTIFYING'
    );

    const updatedChildTable = database.schemas[0].tables.find(
      (t) => t.id === childTableId
    );
    const updatedChildRel = updatedChildTable?.relationships.find(
      (r) => r.id === parentChildRelationshipId
    );

    const updatedGrandchildTable = database.schemas[0].tables.find(
      (t) => t.id === grandchildTableId
    );
    const updatedGrandchildRel = updatedGrandchildTable?.relationships.find(
      (r) => r.id === childGrandchildRelationshipId
    );

    expect(updatedChildRel?.kind).toBe('NON_IDENTIFYING');
    expect(updatedGrandchildRel?.kind).toBe('NON_IDENTIFYING');
    expect(updatedGrandchildRel?.isAffected).toBe(true);
    expect(updatedGrandchildRel).toBeDefined();
    expect(updatedChildTable?.columns.find((c) => c.id === childFkColumnId)).toBeDefined();
  });

  test('존재하지 않는 대상 테이블로는 관계를 생성할 수 없다.', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withId('schema-1').withTable((t) =>
          t
            .withId('child-table')
            .withName('child')
            .withColumn((c) => c.withId('child-col').withName('col'))
        )
      )
      .build();

    const invalidRelationship = createRelationshipBuilder()
      .withFkTableId('child-table')
      .withName('invalid_fk')
      .withKind('NON_IDENTIFYING')
      .withPkTableId('non-existent-table')
      .withColumn((rc) => rc.withPkColumnId('child-col').withPkColumnId('some-col'))
      .build();

    expect(() => ERD_VALIDATOR.createRelationship(database, 'schema-1', invalidRelationship)).toThrow(
      RelationshipTargetTableNotExistError
    );
  });

  describe('순환 참조 검증', () => {
    test('IDENTIFYING 관계에서 직접적인 순환 참조는 금지된다', () => {
      // Step 1: 관계 없는 기본 테이블들 생성
      const db = createTestDatabase()
        .withSchema((s) =>
          s
            .withId('schema-1')
            .withTable((t) =>
              t
                .withId('table-a')
                .withName('table_a')
                .withColumn((c) => c.withId('a-id').withName('id2').withDataType('INT'))
                .withConstraint((c) =>
                  c
                    .withName('pk_a')
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('a-id'))
                )
            )
            .withTable((t) =>
              t
                .withId('table-b')
                .withName('table_b')
                .withColumn((c) => c.withId('b-id').withName('id2').withDataType('INT'))
                .withConstraint((c) =>
                  c
                    .withName('pk_b')
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('b-id'))
                )
            )
        )
        .build();

      // Step 2: 첫 번째 IDENTIFYING 관계 추가 (A -> B)
      const aToBRelationship = createRelationshipBuilder()
        .withFkTableId('table-a')
        .withName('fk_a_to_b')
        .withKind('IDENTIFYING') // IDENTIFYING 관계
        .withPkTableId('table-b')
        .withColumn((rc) => rc.withPkColumnId('b-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', aToBRelationship)).not.toThrow();
      const aToBDb = ERD_VALIDATOR.createRelationship(db, 'schema-1', aToBRelationship);

      // Step 3: IDENTIFYING 관계에서 순환 참조 시도 (B -> A) - 논리적으로 불가능
      const cyclicRelationship = createRelationshipBuilder()
        .withFkTableId('table-b')
        .withName('fk_b_to_a')
        .withKind('IDENTIFYING') // IDENTIFYING 관계에서 순환은 불가능
        .withPkTableId('table-a')
        .withColumn((rc) => rc.withPkColumnId('a-id'))
        .build();

      // IDENTIFYING 관계에서 순환 참조는 금지되어야 함
      expect(() => ERD_VALIDATOR.createRelationship(aToBDb, 'schema-1', cyclicRelationship)).toThrow(
        RelationshipCyclicReferenceError
      );
    });

    test('NON_IDENTIFYING 관계에서는 순환 참조가 허용된다', () => {
      // Step 1: 관계 없는 기본 테이블들 생성
      const db = createTestDatabase()
        .withSchema((s) =>
          s
            .withId('schema-1')
            .withTable((t) =>
              t
                .withId('user-table')
                .withName('user')
                .withColumn((c) => c.withId('user-id').withName('id2').withDataType('INT'))
                .withConstraint((c) =>
                  c
                    .withName('pk_user')
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('user-id'))
                )
            )
            .withTable((t) =>
              t
                .withId('company-table')
                .withName('company')
                .withColumn((c) => c.withId('company-id').withName('id2').withDataType('INT'))
                .withConstraint((c) =>
                  c
                    .withName('pk_company')
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('company-id'))
                )
            )
        )
        .build();

      // Step 2: 첫 번째 NON_IDENTIFYING 관계 추가 (user -> company)
      const userCompanyRelationship = createRelationshipBuilder()
        .withFkTableId('user-table')
        .withName('fk_user_company')
        .withKind('NON_IDENTIFYING') // NON_IDENTIFYING 관계
        .withPkTableId('company-table')
        .withColumn((rc) => rc.withPkColumnId('company-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', userCompanyRelationship)).not.toThrow();

      // Step 3: NON_IDENTIFYING 관계에서 순환 참조 (company -> user) - 허용되어야 함
      const cyclicRelationship = createRelationshipBuilder()
        .withFkTableId('company-table')
        .withName('fk_company_owner')
        .withKind('NON_IDENTIFYING') // NON_IDENTIFYING에서는 순환 허용
        .withPkTableId('user-table')
        .withColumn((rc) => rc.withPkColumnId('user-id'))
        .build();

      // NON_IDENTIFYING 관계에서 순환 참조는 허용되어야 함
      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', cyclicRelationship)).not.toThrow();
    });

    test('IDENTIFYING 관계에서 복합 PK 전파가 올바르게 처리된다', () => {
      // Step 1: 3단계 계층구조 테이블 생성 (Order -> OrderLine -> OrderLineDetail)
      const db = createTestDatabase()
        .withSchema((s) =>
          s
            .withId('schema-1')
            .withTable((t) =>
              t
                .withId('order-table')
                .withName('order')
                .withColumn((c) => c.withId('order-id').withName('id2').withDataType('INT'))
                .withConstraint((c) =>
                  c
                    .withName('pk_order')
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('order-id'))
                )
            )
            .withTable((t) =>
              t
                .withId('order-line-table')
                .withName('order_line')
                .withColumn((c) => c.withId('line-id').withName('line_id').withDataType('INT'))
                .withConstraint((c) =>
                  c
                    .withName('pk_order_line')
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('line-id'))
                )
            )
            .withTable((t) =>
              t
                .withId('order-line-detail-table')
                .withName('order_line_detail')
                .withColumn((c) => c.withId('detail-id').withName('detail_id').withDataType('INT'))
                .withConstraint((c) =>
                  c
                    .withName('pk_order_line_detail')
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('detail-id'))
                )
            )
        )
        .build();

      // Step 2: IDENTIFYING 관계 추가 (Order -> OrderLine)
      // 결과: OrderLine의 PK = (line_id, order_id)
      const orderLineRelationship = createRelationshipBuilder()
        .withFkTableId('order-line-table')
        .withName('fk_order_line')
        .withKind('IDENTIFYING')
        .withPkTableId('order-table')
        .withColumn((rc) => rc.withPkColumnId('order-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', orderLineRelationship)).not.toThrow();
      const orderDb = ERD_VALIDATOR.createRelationship(db, 'schema-1', orderLineRelationship);

      // Step 3: IDENTIFYING 관계 추가 (OrderLine -> OrderLineDetail)
      // 문제: OrderLine의 PK는 이제 복합키 (line_id, order_id)
      // 따라서 OrderLineDetail은 이 복합키 전체를 참조해야 함
      const orderLineDetailRelationship = createRelationshipBuilder()
        .withFkTableId('order-line-detail-table')
        .withName('fk_order_line_detail')
        .withKind('IDENTIFYING')
        .withPkTableId('order-line-table')
        // 복합 PK 참조 - 여러 컬럼이 필요함
        .withColumn((rc) => rc.withPkColumnId('line-id'))
        .withColumn((rc) => rc.withPkColumnId('order-id')) // 전파된 PK도 포함, columnId는 상위 테이블의 PK 컬럼 ID, 내부적으로 관리하는 ID는 자동 생성
        .build();

      // 복합 PK를 올바르게 참조하는 IDENTIFYING 관계는 허용되어야 함
      expect(() => ERD_VALIDATOR.createRelationship(orderDb, 'schema-1', orderLineDetailRelationship)).not.toThrow();
      const orderLineDetailDb = ERD_VALIDATOR.createRelationship(orderDb, 'schema-1', orderLineDetailRelationship);

      const orderLineDetailTable = orderLineDetailDb.schemas[0].tables.find((t) => t.id === 'order-line-detail-table');
      expect(orderLineDetailTable).toBeDefined();
      if (orderLineDetailTable) {
        const pkConstraint = orderLineDetailTable.constraints.find((c) => c.kind === 'PRIMARY_KEY');
        expect(pkConstraint).toBeDefined();
        if (pkConstraint) {
          expect(pkConstraint.columns.length).toBe(3); // detail_id, line_id, order_id
        }
      }
    });

    test('엣지 케이스: 동일한 테이블 쌍 간에 여러 관계가 있을 때 순환 검증', () => {
      // Step 1: 기본 테이블 생성 (외래키 컬럼들 없이)
      const db = createTestDatabase()
        .withSchema((s) =>
          s.withId('schema-1').withTable((t) =>
            t
              .withId('user-table')
              .withName('user')
              .withColumn((c) => c.withId('user-id').withName('id2').withDataType('INT'))
              .withConstraint((c) =>
                c
                  .withName('pk_user')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId('user-id'))
              )
          )
        )
        .build();

      // Step 2: 첫 번째 자기 참조 관계 추가 (created_by_user_id 컬럼이 추가됨)
      const createdByRelationship = createRelationshipBuilder()
        .withFkTableId('user-table')
        .withName('fk_user_created_by')
        .withKind('NON_IDENTIFYING')
        .withPkTableId('user-table')
        .withColumn((rc) => rc.withPkColumnId('user-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', createdByRelationship)).not.toThrow();

      // Step 3: 두 번째 자기 참조 관계 추가 (updated_by_user_id 컬럼이 추가됨)
      const updatedByRelationship = createRelationshipBuilder()
        .withFkTableId('user-table')
        .withName('fk_user_updated_by')
        .withKind('NON_IDENTIFYING')
        .withPkTableId('user-table')
        .withColumn((rc) => rc.withPkColumnId('user-id'))
        .build();

      // 같은 테이블에 대한 여러 자기 참조 관계는 허용되어야 함
      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', updatedByRelationship)).not.toThrow();
    });

    describe('다단계 관계 체인에서 순환 검증', () => {
      test('4단계 체인: 중간 관계를 IDENTIFYING으로 변경 시 순환이 발생하면 에러', () => {
        const schemaId = 'schema-1';

        // 4개 테이블 생성
        const db = createTestDatabase()
          .withSchema((s) =>
            s
              .withId(schemaId)
              .withTable((t) =>
                t
                  .withId('table-a')
                  .withName('table_a')
                  .withColumn((c) => c.withId('a-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_a').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('a-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-b')
                  .withName('table_b')
                  .withColumn((c) => c.withId('b-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_b').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('b-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-c')
                  .withName('table_c')
                  .withColumn((c) => c.withId('c-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_c').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('c-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-d')
                  .withName('table_d')
                  .withColumn((c) => c.withId('d-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_d').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('d-id'))
                  )
              )
          )
          .build();

        // Step 1: 모든 관계를 NON_IDENTIFYING으로 먼저 생성
        // B → A (NON_IDENTIFYING)
        let currentDb = ERD_VALIDATOR.createRelationship(
          db,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-b-a')
            .withFkTableId('table-b')
            .withPkTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('a-id'))
            .build()
        );

        // C → B (NON_IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withFkTableId('table-c')
            .withPkTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('b-id'))
            .build()
        );

        // D → C (NON_IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withFkTableId('table-d')
            .withPkTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('c-id'))
            .build()
        );

        // A → D (NON_IDENTIFYING) - 순환 고리 완성 (but NON_ID라 허용)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-a-d')
            .withFkTableId('table-a')
            .withPkTableId('table-d')
            .withName('fk_a_to_d')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('d-id'))
            .build()
        );

        // Step 2: 순차적으로 IDENTIFYING으로 변경
        // B→A: IDENTIFYING으로 변경 (OK)
        currentDb = ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-b-a', 'IDENTIFYING');

        // D→C: IDENTIFYING으로 변경 (OK)
        currentDb = ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-d-c', 'IDENTIFYING');

        // A→D: IDENTIFYING으로 변경 (OK - 아직 C→B가 NON_ID라 순환 끊김)
        currentDb = ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-a-d', 'IDENTIFYING');

        // Step 3: C→B를 IDENTIFYING으로 변경 시도 → 순환 에러 발생해야 함
        expect(() =>
          ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-c-b', 'IDENTIFYING')
        ).toThrow(RelationshipCyclicReferenceError);
      });

      test('5단계 체인: 중간 관계를 IDENTIFYING으로 변경 시 순환이 발생하면 에러', () => {
        const schemaId = 'schema-1';

        const db = createTestDatabase()
          .withSchema((s) =>
            s
              .withId(schemaId)
              .withTable((t) =>
                t
                  .withId('table-a')
                  .withName('table_a')
                  .withColumn((c) => c.withId('a-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_a').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('a-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-b')
                  .withName('table_b')
                  .withColumn((c) => c.withId('b-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_b').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('b-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-c')
                  .withName('table_c')
                  .withColumn((c) => c.withId('c-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_c').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('c-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-d')
                  .withName('table_d')
                  .withColumn((c) => c.withId('d-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_d').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('d-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-e')
                  .withName('table_e')
                  .withColumn((c) => c.withId('e-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_e').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('e-id'))
                  )
              )
          )
          .build();

        // 모든 관계를 NON_IDENTIFYING으로 먼저 생성
        let currentDb = ERD_VALIDATOR.createRelationship(
          db,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-b-a')
            .withFkTableId('table-b')
            .withPkTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('a-id'))
            .build()
        );

        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withFkTableId('table-c')
            .withPkTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('b-id'))
            .build()
        );

        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withFkTableId('table-d')
            .withPkTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('c-id'))
            .build()
        );

        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-e-d')
            .withFkTableId('table-e')
            .withPkTableId('table-d')
            .withName('fk_e_to_d')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('d-id'))
            .build()
        );

        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-a-e')
            .withFkTableId('table-a')
            .withPkTableId('table-e')
            .withName('fk_a_to_e')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('e-id'))
            .build()
        );

        // 순차적으로 IDENTIFYING으로 변경 (마지막 하나 빼고)
        currentDb = ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-b-a', 'IDENTIFYING');
        currentDb = ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-d-c', 'IDENTIFYING');
        currentDb = ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-e-d', 'IDENTIFYING');
        currentDb = ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-a-e', 'IDENTIFYING');

        // C→B를 IDENTIFYING으로 변경 시도 → 순환 에러 발생해야 함
        expect(() =>
          ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-c-b', 'IDENTIFYING')
        ).toThrow(RelationshipCyclicReferenceError);
      });

      test('분기 구조: 여러 경로 중 하나가 순환을 형성하면 에러', () => {
        const schemaId = 'schema-1';

        const db = createTestDatabase()
          .withSchema((s) =>
            s
              .withId(schemaId)
              .withTable((t) =>
                t
                  .withId('table-a')
                  .withName('table_a')
                  .withColumn((c) => c.withId('a-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_a').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('a-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-b')
                  .withName('table_b')
                  .withColumn((c) => c.withId('b-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_b').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('b-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-c')
                  .withName('table_c')
                  .withColumn((c) => c.withId('c-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_c').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('c-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-d')
                  .withName('table_d')
                  .withColumn((c) => c.withId('d-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_d').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('d-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-e')
                  .withName('table_e')
                  .withColumn((c) => c.withId('e-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_e').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('e-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-f')
                  .withName('table_f')
                  .withColumn((c) => c.withId('f-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_f').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('f-id'))
                  )
              )
          )
          .build();

        // B → A (IDENTIFYING)
        let currentDb = ERD_VALIDATOR.createRelationship(
          db,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-b-a')
            .withFkTableId('table-b')
            .withPkTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('a-id'))
            .build()
        );

        // C → B (IDENTIFYING) - 분기 1
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withFkTableId('table-c')
            .withPkTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('b-id'))
            .build()
        );

        // D → C (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withFkTableId('table-d')
            .withPkTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('c-id'))
            .build()
        );

        // E → B (IDENTIFYING) - 분기 2
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-e-b')
            .withFkTableId('table-e')
            .withPkTableId('table-b')
            .withName('fk_e_to_b')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('b-id'))
            .build()
        );

        // F → E (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-f-e')
            .withFkTableId('table-f')
            .withPkTableId('table-e')
            .withName('fk_f_to_e')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('e-id'))
            .build()
        );

        // A → F (NON_IDENTIFYING) - 순환 끊는 지점
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-a-f')
            .withFkTableId('table-a')
            .withPkTableId('table-f')
            .withName('fk_a_to_f')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('f-id'))
            .build()
        );

        // A-F 관계를 IDENTIFYING으로 변경 시도 → 순환 에러 발생해야 함
        expect(() =>
          ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-a-f', 'IDENTIFYING')
        ).toThrow(RelationshipCyclicReferenceError);
      });

      test('순환 없는 4단계 체인: IDENTIFYING으로 변경 허용', () => {
        const schemaId = 'schema-1';

        const db = createTestDatabase()
          .withSchema((s) =>
            s
              .withId(schemaId)
              .withTable((t) =>
                t
                  .withId('table-a')
                  .withName('table_a')
                  .withColumn((c) => c.withId('a-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_a').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('a-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-b')
                  .withName('table_b')
                  .withColumn((c) => c.withId('b-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_b').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('b-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-c')
                  .withName('table_c')
                  .withColumn((c) => c.withId('c-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_c').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('c-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-d')
                  .withName('table_d')
                  .withColumn((c) => c.withId('d-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_d').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('d-id'))
                  )
              )
          )
          .build();

        // B → A (IDENTIFYING)
        let currentDb = ERD_VALIDATOR.createRelationship(
          db,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-b-a')
            .withFkTableId('table-b')
            .withPkTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('a-id'))
            .build()
        );

        // C → B (NON_IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withFkTableId('table-c')
            .withPkTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('b-id'))
            .build()
        );

        // D → C (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withFkTableId('table-d')
            .withPkTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('c-id'))
            .build()
        );

        // C-B 관계를 IDENTIFYING으로 변경 → 순환 없으므로 허용
        expect(() =>
          ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-c-b', 'IDENTIFYING')
        ).not.toThrow();

        const updatedDb = ERD_VALIDATOR.changeRelationshipKind(currentDb, schemaId, 'rel-c-b', 'IDENTIFYING');
        const tableC = updatedDb.schemas[0].tables.find((t) => t.id === 'table-c');
        const relCB = tableC?.relationships.find((r) => r.id === 'rel-c-b');
        expect(relCB?.kind).toBe('IDENTIFYING');
      });

      test('createRelationship: 4단계 체인에서 마지막 IDENTIFYING 관계 생성 시 순환이 발생하면 에러', () => {
        const schemaId = 'schema-1';

        const db = createTestDatabase()
          .withSchema((s) =>
            s
              .withId(schemaId)
              .withTable((t) =>
                t
                  .withId('table-a')
                  .withName('table_a')
                  .withColumn((c) => c.withId('a-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_a').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('a-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-b')
                  .withName('table_b')
                  .withColumn((c) => c.withId('b-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_b').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('b-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-c')
                  .withName('table_c')
                  .withColumn((c) => c.withId('c-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_c').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('c-id'))
                  )
              )
              .withTable((t) =>
                t
                  .withId('table-d')
                  .withName('table_d')
                  .withColumn((c) => c.withId('d-id').withName('id').withDataType('INT'))
                  .withConstraint((c) =>
                    c.withName('pk_d').withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('d-id'))
                  )
              )
          )
          .build();

        // B → A (IDENTIFYING)
        let currentDb = ERD_VALIDATOR.createRelationship(
          db,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-b-a')
            .withFkTableId('table-b')
            .withPkTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('a-id'))
            .build()
        );

        // C → B (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withFkTableId('table-c')
            .withPkTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('b-id'))
            .build()
        );

        // D → C (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withFkTableId('table-d')
            .withPkTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withPkColumnId('c-id'))
            .build()
        );

        // A → D (IDENTIFYING) - 순환 발생
        expect(() =>
          ERD_VALIDATOR.createRelationship(
            currentDb,
            schemaId,
            createRelationshipBuilder()
              .withId('rel-a-d')
              .withFkTableId('table-a')
              .withPkTableId('table-d')
              .withName('fk_a_to_d')
              .withKind('IDENTIFYING')
              .withColumn((rc) => rc.withPkColumnId('d-id'))
              .build()
          )
        ).toThrow(RelationshipCyclicReferenceError);
      });
    });
  });

  describe('관계 삭제 검증', () => {
    test('식별 관계 삭제 시 자식 테이블의 PK 순번이 재정렬된다', () => {
      const schemaId = 'schema-1';
      const parentTableId = 'parent-table';
      const childTableId = 'child-table';
      const parentColumnId = 'parent-id';
      const childOwnPkColumnId = 'child-own-id';
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
                  c.withId(childOwnPkColumnId).withName('id').withDataType('INT')
                )
                .withConstraint((c) =>
                  c
                    .withName('pk_child')
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId(childOwnPkColumnId).withSeqNo(0))
                )
            )
        )
        .build();

      const relationship = createRelationshipBuilder()
        .withId('rel-1')
        .withFkTableId(childTableId)
        .withName('fk_parent')
        .withKind('IDENTIFYING')
        .withPkTableId(parentTableId)
        .withColumn((rc) =>
          rc.withFkColumnId(fkColumnId).withPkColumnId(parentColumnId)
        )
        .build();

      const databaseWithRelationship = ERD_VALIDATOR.createRelationship(
        baseDatabase,
        schemaId,
        relationship
      );

      const updatedDatabase = ERD_VALIDATOR.deleteRelationship(
        databaseWithRelationship,
        schemaId,
        relationship.id
      );

      const childTable = updatedDatabase.schemas[0].tables.find((t: Table) => t.id === childTableId);
      const pkConstraint = childTable?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');

      expect(pkConstraint).toBeDefined();
      expect(pkConstraint?.columns).toHaveLength(1);
      expect(pkConstraint?.columns[0].seqNo).toBe(0);
      expect(pkConstraint?.columns[0].columnId).toBe(childOwnPkColumnId);
    });

    test('중간 PK 컬럼 삭제 시 seqNo가 재정렬된다', () => {
      const schemaId = 'schema-1';
      const parentTableId = 'parent-table';
      const childTableId = 'child-table';

      const baseDatabase = createTestDatabase()
        .withSchema((s) =>
          s
            .withId(schemaId)
            .withTable((t) =>
              t
                .withId(parentTableId)
                .withName('parent')
                .withColumn((c) => c.withId('p1').withName('p1').withDataType('INT'))
                .withConstraint((c) => c.withKind('PRIMARY_KEY').withColumn((cc) => cc.withColumnId('p1')))
            )
            .withTable((t) =>
              t
                .withId(childTableId)
                .withName('child')
                .withColumn((c) => c.withId('c1').withName('c1').withDataType('INT'))
                .withColumn((c) => c.withId('c2').withName('c2').withDataType('INT'))
                .withConstraint((c) =>
                  c.withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('c1').withSeqNo(0))
                    .withColumn((cc) => cc.withColumnId('c2').withSeqNo(1))
                )
            )
        )
        .build();

      const relationship = createRelationshipBuilder()
        .withId('rel-1')
        .withFkTableId(childTableId)
        .withName('fk_parent')
        .withKind('IDENTIFYING')
        .withPkTableId(parentTableId)
        .withColumn((rc) =>
          rc.withFkColumnId('fk_p1').withPkColumnId('p1')
        )
        .build();

      let database = ERD_VALIDATOR.createRelationship(baseDatabase, schemaId, relationship);
      database = ERD_VALIDATOR.deleteRelationship(database, schemaId, relationship.id);

      const childTable = database.schemas[0].tables.find((t: Table) => t.id === childTableId);
      const pkConstraint = childTable?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');

      expect(pkConstraint?.columns).toHaveLength(2);
      expect(pkConstraint?.columns.find(c => c.columnId === 'c1')?.seqNo).toBe(0);
      expect(pkConstraint?.columns.find(c => c.columnId === 'c2')?.seqNo).toBe(1);
    });
  });

  describe('Cascade PK constraint removal when changing to NON_IDENTIFYING', () => {
    test('Table1 -> Table2 (IDENTIFYING) -> Table3 (IDENTIFYING) 에서 Table1-Table2를 NON_IDENTIFYING으로 변경하면 Table3의 PK도 업데이트되어야 한다', () => {
      const schemaId = 'schema-1';
      const table1Id = 'table1';
      const table2Id = 'table2';
      const table3Id = 'table3';

      const baseDatabase = createTestDatabase()
        .withSchema((s) =>
          s
            .withId(schemaId)
            .withTable((t) =>
              t
                .withId(table1Id)
                .withName('Table1')
                .withColumn((c) => c.withId('t1-col1').withName('Column1'))
                .withConstraint((c) =>
                  c
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('t1-col1').withSeqNo(0))
                )
            )
            .withTable((t) => t.withId(table2Id).withName('Table2'))
            .withTable((t) => t.withId(table3Id).withName('Table3'))
        )
        .build();

      const rel1 = createRelationshipBuilder()
        .withId('rel-table2-table1')
        .withFkTableId(table2Id)
        .withName('Table2_Table1')
        .withKind('IDENTIFYING')
        .withPkTableId(table1Id)
        .withColumn((rc) => rc.withPkColumnId('t1-col1'))
        .build();

      let database = ERD_VALIDATOR.createRelationship(baseDatabase, schemaId, rel1);

      let table2 = database.schemas[0].tables.find((t: Table) => t.id === table2Id);
      let table2PkConstraint = table2?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');
      const table2FkColumnId = table2?.columns.find((c) => c.name === 'Table1_Column1')?.id;

      expect(table2PkConstraint).toBeDefined();
      expect(table2PkConstraint?.columns).toHaveLength(1);
      expect(table2PkConstraint?.columns[0].columnId).toBe(table2FkColumnId);

      const rel2 = createRelationshipBuilder()
        .withId('rel-table3-table2')
        .withFkTableId(table3Id)
        .withName('Table3_Table2')
        .withKind('IDENTIFYING')
        .withPkTableId(table2Id)
        .withColumn((rc) => rc.withPkColumnId(table2FkColumnId!))
        .build();

      database = ERD_VALIDATOR.createRelationship(database, schemaId, rel2);

      let table3 = database.schemas[0].tables.find((t: Table) => t.id === table3Id);
      let table3PkConstraint = table3?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');
      const table3FkColumnId = table3?.columns.find((c) => c.name.includes('Table1_Column1'))?.id;

      expect(table3PkConstraint).toBeDefined();
      expect(table3PkConstraint?.columns).toHaveLength(1);
      expect(table3PkConstraint?.columns[0].columnId).toBe(table3FkColumnId);

      database = ERD_VALIDATOR.changeRelationshipKind(database, schemaId, 'rel-table2-table1', 'NON_IDENTIFYING');

      table2 = database.schemas[0].tables.find((t: Table) => t.id === table2Id);
      table2PkConstraint = table2?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');

      expect(table2PkConstraint).toBeUndefined();

      table3 = database.schemas[0].tables.find((t: Table) => t.id === table3Id);
      table3PkConstraint = table3?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');

      const table3ColumnExists = table3?.columns.find((c) => c.id === table3FkColumnId);
      expect(table3ColumnExists).toBeDefined();

      expect(table3PkConstraint).toBeUndefined();
    });

    test('Table1 -> Table2 (IDENTIFYING) -> Table3 (IDENTIFYING) 에서 Table2가 다른 PK를 가지고 있으면 해당 PK는 유지되어야 한다', () => {
      const schemaId = 'schema-1';
      const table1Id = 'table1';
      const table2Id = 'table2';
      const table3Id = 'table3';

      const baseDatabase = createTestDatabase()
        .withSchema((s) =>
          s
            .withId(schemaId)
            .withTable((t) =>
              t
                .withId(table1Id)
                .withName('Table1')
                .withColumn((c) => c.withId('t1-col1').withName('Column1'))
                .withConstraint((c) =>
                  c
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('t1-col1').withSeqNo(0))
                )
            )
            .withTable((t) =>
              t
                .withId(table2Id)
                .withName('Table2')
                .withColumn((c) => c.withId('t2-own-pk').withName('OwnPK'))
                .withConstraint((c) =>
                  c
                    .withKind('PRIMARY_KEY')
                    .withColumn((cc) => cc.withColumnId('t2-own-pk').withSeqNo(0))
                )
            )
            .withTable((t) => t.withId(table3Id).withName('Table3'))
        )
        .build();

      const rel1 = createRelationshipBuilder()
        .withId('rel-table2-table1')
        .withFkTableId(table2Id)
        .withName('Table2_Table1')
        .withKind('IDENTIFYING')
        .withPkTableId(table1Id)
        .withColumn((rc) => rc.withPkColumnId('t1-col1'))
        .build();

      let database = ERD_VALIDATOR.createRelationship(baseDatabase, schemaId, rel1);

      let table2 = database.schemas[0].tables.find((t: Table) => t.id === table2Id);
      let table2PkConstraint = table2?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');
      const table2FkColumnId = table2?.columns.find((c) => c.name === 'Table1_Column1')?.id;

      expect(table2PkConstraint).toBeDefined();
      expect(table2PkConstraint?.columns).toHaveLength(2);
      expect(table2PkConstraint?.columns.find((c) => c.columnId === 't2-own-pk')).toBeDefined();
      expect(table2PkConstraint?.columns.find((c) => c.columnId === table2FkColumnId)).toBeDefined();

      const rel2 = createRelationshipBuilder()
        .withId('rel-table3-table2')
        .withFkTableId(table3Id)
        .withName('Table3_Table2')
        .withKind('IDENTIFYING')
        .withPkTableId(table2Id)
        .withColumn((rc) => rc.withPkColumnId('t2-own-pk'))
        .build();

      database = ERD_VALIDATOR.createRelationship(database, schemaId, rel2);

      database = ERD_VALIDATOR.changeRelationshipKind(database, schemaId, 'rel-table2-table1', 'NON_IDENTIFYING');

      table2 = database.schemas[0].tables.find((t: Table) => t.id === table2Id);
      table2PkConstraint = table2?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');

      expect(table2PkConstraint).toBeDefined();
      expect(table2PkConstraint?.columns).toHaveLength(1);
      expect(table2PkConstraint?.columns[0].columnId).toBe('t2-own-pk');

      const table3 = database.schemas[0].tables.find((t: Table) => t.id === table3Id);
      const table3PkConstraint = table3?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');
      const table3FkColumn = table3?.columns.find((c) => c.name === 'Table2_OwnPK');
      const table3InheritedColumn = table3?.columns.find(
        (c) => c.name === 'Table2_Table1_Column1'
      );

      expect(table3FkColumn).toBeDefined();
      expect(table3InheritedColumn).toBeDefined();
      expect(table3PkConstraint).toBeDefined();
      expect(table3PkConstraint?.columns).toHaveLength(1);
      expect(table3PkConstraint?.columns[0].columnId).toBe(table3FkColumn?.id);
      expect(
        table3PkConstraint?.columns.some(
          (c) => c.columnId === table3InheritedColumn?.id
        )
      ).toBe(false);
    });
  });
});
