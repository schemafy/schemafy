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

    // createRelationship을 하는 경우에, 외래키 컬럼은 자동으로 생성됨. withRefColumnId만 지정하면 됨. (가정)
    const relationship = createRelationshipBuilder()
      .withSrcTableId('child-table')
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('parent-table')
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
      .withSrcTableId('employee-table')
      .withName('fk_employee')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('employee-table')
      .withColumn((rc) => rc.withRefColumnId('id-col'))
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
      .withSrcTableId('child-table')
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('parent-table')
      .withColumn((rc) => rc.withRefColumnId('parent-id'))
      .build();

    let duplicateDatabase = database;

    expect(
      () => (duplicateDatabase = ERD_VALIDATOR.createRelationship(database, 'schema-1', relationship))
    ).not.toThrow();

    const duplicateRelationship = createRelationshipBuilder()
      .withSrcTableId('child-table')
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('parent-table')
      .withColumn((rc) => rc.withRefColumnId('parent-id'))
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
      .withSrcTableId(childTableId)
      .withName('fk_parent')
      .withKind('IDENTIFYING')
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
      .withSrcTableId(childTableId)
      .withName('fk_parent')
      .withKind('IDENTIFYING')
      .withTgtTableId(parentTableId)
      .withColumn((rc) =>
        rc.withFkColumnId(childFkColumnId).withRefColumnId(parentPkColumnId)
      )
      .build();

    const databaseWithParentRelationship = ERD_VALIDATOR.createRelationship(
      baseDatabase,
      schemaId,
      parentChildRelationship
    );

    const grandchildRelationship = createRelationshipBuilder()
      .withId(grandchildRelationshipId)
      .withSrcTableId(grandchildTableId)
      .withName('fk_child_parent')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId(childTableId)
      .withColumn((rc) =>
        rc
          .withFkColumnId(grandchildFkColumnId)
          .withRefColumnId(childFkColumnId)
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
      .withSrcTableId('child-table')
      .withName('invalid_fk')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('non-existent-table')
      .withColumn((rc) => rc.withRefColumnId('child-col').withRefColumnId('some-col'))
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
        .withSrcTableId('table-a')
        .withName('fk_a_to_b')
        .withKind('IDENTIFYING') // IDENTIFYING 관계
        .withTgtTableId('table-b')
        .withColumn((rc) => rc.withRefColumnId('b-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', aToBRelationship)).not.toThrow();
      const aToBDb = ERD_VALIDATOR.createRelationship(db, 'schema-1', aToBRelationship);

      // Step 3: IDENTIFYING 관계에서 순환 참조 시도 (B -> A) - 논리적으로 불가능
      const cyclicRelationship = createRelationshipBuilder()
        .withSrcTableId('table-b')
        .withName('fk_b_to_a')
        .withKind('IDENTIFYING') // IDENTIFYING 관계에서 순환은 불가능
        .withTgtTableId('table-a')
        .withColumn((rc) => rc.withRefColumnId('a-id'))
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
        .withSrcTableId('user-table')
        .withName('fk_user_company')
        .withKind('NON_IDENTIFYING') // NON_IDENTIFYING 관계
        .withTgtTableId('company-table')
        .withColumn((rc) => rc.withRefColumnId('company-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', userCompanyRelationship)).not.toThrow();

      // Step 3: NON_IDENTIFYING 관계에서 순환 참조 (company -> user) - 허용되어야 함
      const cyclicRelationship = createRelationshipBuilder()
        .withSrcTableId('company-table')
        .withName('fk_company_owner')
        .withKind('NON_IDENTIFYING') // NON_IDENTIFYING에서는 순환 허용
        .withTgtTableId('user-table')
        .withColumn((rc) => rc.withRefColumnId('user-id'))
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
        .withSrcTableId('order-line-table')
        .withName('fk_order_line')
        .withKind('IDENTIFYING')
        .withTgtTableId('order-table')
        .withColumn((rc) => rc.withRefColumnId('order-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', orderLineRelationship)).not.toThrow();
      const orderDb = ERD_VALIDATOR.createRelationship(db, 'schema-1', orderLineRelationship);

      // Step 3: IDENTIFYING 관계 추가 (OrderLine -> OrderLineDetail)
      // 문제: OrderLine의 PK는 이제 복합키 (line_id, order_id)
      // 따라서 OrderLineDetail은 이 복합키 전체를 참조해야 함
      const orderLineDetailRelationship = createRelationshipBuilder()
        .withSrcTableId('order-line-detail-table')
        .withName('fk_order_line_detail')
        .withKind('IDENTIFYING')
        .withTgtTableId('order-line-table')
        // 복합 PK 참조 - 여러 컬럼이 필요함
        .withColumn((rc) => rc.withRefColumnId('line-id'))
        .withColumn((rc) => rc.withRefColumnId('order-id')) // 전파된 PK도 포함, columnId는 상위 테이블의 PK 컬럼 ID, 내부적으로 관리하는 ID는 자동 생성
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
        .withSrcTableId('user-table')
        .withName('fk_user_created_by')
        .withKind('NON_IDENTIFYING')
        .withTgtTableId('user-table')
        .withColumn((rc) => rc.withRefColumnId('user-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', createdByRelationship)).not.toThrow();

      // Step 3: 두 번째 자기 참조 관계 추가 (updated_by_user_id 컬럼이 추가됨)
      const updatedByRelationship = createRelationshipBuilder()
        .withSrcTableId('user-table')
        .withName('fk_user_updated_by')
        .withKind('NON_IDENTIFYING')
        .withTgtTableId('user-table')
        .withColumn((rc) => rc.withRefColumnId('user-id'))
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
            .withSrcTableId('table-b')
            .withTgtTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('a-id'))
            .build()
        );

        // C → B (NON_IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withSrcTableId('table-c')
            .withTgtTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('b-id'))
            .build()
        );

        // D → C (NON_IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withSrcTableId('table-d')
            .withTgtTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('c-id'))
            .build()
        );

        // A → D (NON_IDENTIFYING) - 순환 고리 완성 (but NON_ID라 허용)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-a-d')
            .withSrcTableId('table-a')
            .withTgtTableId('table-d')
            .withName('fk_a_to_d')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('d-id'))
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
            .withSrcTableId('table-b')
            .withTgtTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('a-id'))
            .build()
        );

        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withSrcTableId('table-c')
            .withTgtTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('b-id'))
            .build()
        );

        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withSrcTableId('table-d')
            .withTgtTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('c-id'))
            .build()
        );

        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-e-d')
            .withSrcTableId('table-e')
            .withTgtTableId('table-d')
            .withName('fk_e_to_d')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('d-id'))
            .build()
        );

        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-a-e')
            .withSrcTableId('table-a')
            .withTgtTableId('table-e')
            .withName('fk_a_to_e')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('e-id'))
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
            .withSrcTableId('table-b')
            .withTgtTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('a-id'))
            .build()
        );

        // C → B (IDENTIFYING) - 분기 1
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withSrcTableId('table-c')
            .withTgtTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('b-id'))
            .build()
        );

        // D → C (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withSrcTableId('table-d')
            .withTgtTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('c-id'))
            .build()
        );

        // E → B (IDENTIFYING) - 분기 2
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-e-b')
            .withSrcTableId('table-e')
            .withTgtTableId('table-b')
            .withName('fk_e_to_b')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('b-id'))
            .build()
        );

        // F → E (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-f-e')
            .withSrcTableId('table-f')
            .withTgtTableId('table-e')
            .withName('fk_f_to_e')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('e-id'))
            .build()
        );

        // A → F (NON_IDENTIFYING) - 순환 끊는 지점
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-a-f')
            .withSrcTableId('table-a')
            .withTgtTableId('table-f')
            .withName('fk_a_to_f')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('f-id'))
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
            .withSrcTableId('table-b')
            .withTgtTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('a-id'))
            .build()
        );

        // C → B (NON_IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withSrcTableId('table-c')
            .withTgtTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('NON_IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('b-id'))
            .build()
        );

        // D → C (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withSrcTableId('table-d')
            .withTgtTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('c-id'))
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
            .withSrcTableId('table-b')
            .withTgtTableId('table-a')
            .withName('fk_b_to_a')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('a-id'))
            .build()
        );

        // C → B (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-c-b')
            .withSrcTableId('table-c')
            .withTgtTableId('table-b')
            .withName('fk_c_to_b')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('b-id'))
            .build()
        );

        // D → C (IDENTIFYING)
        currentDb = ERD_VALIDATOR.createRelationship(
          currentDb,
          schemaId,
          createRelationshipBuilder()
            .withId('rel-d-c')
            .withSrcTableId('table-d')
            .withTgtTableId('table-c')
            .withName('fk_d_to_c')
            .withKind('IDENTIFYING')
            .withColumn((rc) => rc.withRefColumnId('c-id'))
            .build()
        );

        // A → D (IDENTIFYING) - 순환 발생
        expect(() =>
          ERD_VALIDATOR.createRelationship(
            currentDb,
            schemaId,
            createRelationshipBuilder()
              .withId('rel-a-d')
              .withSrcTableId('table-a')
              .withTgtTableId('table-d')
              .withName('fk_a_to_d')
              .withKind('IDENTIFYING')
              .withColumn((rc) => rc.withRefColumnId('d-id'))
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
                    .withColumn((cc) => cc.withColumnId(childOwnPkColumnId).withSeqNo(1))
                )
            )
        )
        .build();

      const relationship = createRelationshipBuilder()
        .withId('rel-1')
        .withSrcTableId(childTableId)
        .withName('fk_parent')
        .withKind('IDENTIFYING')
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

      const updatedDatabase = ERD_VALIDATOR.deleteRelationship(
        databaseWithRelationship,
        schemaId,
        relationship.id
      );

      const childTable = updatedDatabase.schemas[0].tables.find((t: Table) => t.id === childTableId);
      const pkConstraint = childTable?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');

      expect(pkConstraint).toBeDefined();
      expect(pkConstraint?.columns).toHaveLength(1);
      expect(pkConstraint?.columns[0].seqNo).toBe(1);
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
                    .withColumn((cc) => cc.withColumnId('c1').withSeqNo(1))
                    .withColumn((cc) => cc.withColumnId('c2').withSeqNo(2))
                )
            )
        )
        .build();

      const relationship = createRelationshipBuilder()
        .withId('rel-1')
        .withSrcTableId(childTableId)
        .withName('fk_parent')
        .withKind('IDENTIFYING')
        .withTgtTableId(parentTableId)
        .withColumn((rc) =>
          rc.withFkColumnId('fk_p1').withRefColumnId('p1')
        )
        .build();

      let database = ERD_VALIDATOR.createRelationship(baseDatabase, schemaId, relationship);
      database = ERD_VALIDATOR.deleteRelationship(database, schemaId, relationship.id);

      const childTable = database.schemas[0].tables.find((t: Table) => t.id === childTableId);
      const pkConstraint = childTable?.constraints.find((c: Constraint) => c.kind === 'PRIMARY_KEY');

      expect(pkConstraint?.columns).toHaveLength(2);
      expect(pkConstraint?.columns.find(c => c.columnId === 'c1')?.seqNo).toBe(1);
      expect(pkConstraint?.columns.find(c => c.columnId === 'c2')?.seqNo).toBe(2);
    });
  });
});
