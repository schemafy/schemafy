import { createRelationshipBuilder, createTestDatabase } from '../src/lib/builder';
import {
  RelationshipEmptyError,
  RelationshipNameNotUniqueError,
  RelationshipColumnMappingDuplicateError,
  RelationshipTargetTableNotExistError,
  RelationshipColumnTypeIncompatibleError,
  RelationshipDeleteSetNullError,
  RelationshipCyclicReferenceError,
  IdentifyingRelationshipOrderError,
  ERD_VALIDATOR,
} from '../src';

describe('Relationship validation', () => {
  test.skip('관계는 반드시 하나 이상의 컬럼 매핑을 가져야 한다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s
          .withId('schema-1')
          .withTable((t) =>
            t
              .withId('parent-table')
              .withName('parent')
              .withColumn((c) => c.withId('id-col').withName('id'))
          )
          .withTable((t) => t.withId('child-table').withName('child'))
      )
      .build();

    // createRelationship을 하는 경우에, 외래키 컬럼은 자동으로 생성됨. withRefColumnId만 지정하면 됨.
    const relationship = createRelationshipBuilder()
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('parent-table')
      .withColumn((rc) => rc.withRefColumnId('id-col'))
      .build();

    // TODO: 테이블 하나만 받는데, 기준이 뭔지? 두 테이블을 받거나, 아니면 릴레이션 정보를 하나만 받아야하지 싶음.
    expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', 'child-table', relationship)).toThrow(
      RelationshipEmptyError
    );
  });

  test.skip('테이블이 자기 자신을 참조하는 관계를 맺을 수 있다', () => {
    const db = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('employee-table')
            .withName('employee')
            .withColumn((c) => c.withId('id-col').withName('id'))
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
      .withName('fk_employee')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('employee-table')
      .withColumn((rc) => rc.withRefColumnId('id-col'))
      .build();

    // TODO: 이 경우에는 참조하는 테이블이 자기 자신이므로 컬럼을 추가하고 릴레이션을 추가하는데, 내부적으로 중복이 생기면 안된다.
    expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', 'employee-table', relationship)).not.toThrow();
  });

  test.skip('기존 테이블에 중복된 이름의 관계를 추가할 수 없다.', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s
          .withTable((t) =>
            t
              .withId('parent-table')
              .withName('parent')
              .withColumn((c) => c.withId('parent-id').withName('id'))
          )
          .withTable((t) =>
            t
              .withId('child-table')
              .withName('child')
              .withColumn((c) => c.withId('child-id').withName('id'))
          )
      )
      .build();

    const relationship = createRelationshipBuilder()
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('parent-table')
      .withColumn((rc) => rc.withRefColumnId('parent-id'))
      .build();

    expect(() => ERD_VALIDATOR.createRelationship(database, 'schema-1', 'child-table', relationship)).not.toThrow();

    const duplicateRelationship = createRelationshipBuilder()
      .withName('fk_parent')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('parent-table')
      .withColumn((rc) => rc.withRefColumnId('parent-id'))
      .build();

    expect(() => ERD_VALIDATOR.createRelationship(database, 'schema-1', 'child-table', duplicateRelationship)).toThrow(
      RelationshipNameNotUniqueError
    );
  });

  test.skip('존재하지 않는 대상 테이블로는 관계를 생성할 수 없다.', () => {
    const database = createTestDatabase()
      .withSchema((s) =>
        s.withTable((t) =>
          t
            .withId('child-table')
            .withName('child')
            .withColumn((c) => c.withId('child-col').withName('col'))
        )
      )
      .build();

    const invalidRelationship = createRelationshipBuilder()
      .withName('invalid_fk')
      .withKind('NON_IDENTIFYING')
      .withTgtTableId('non-existent-table')
      .withColumn((rc) => rc.withRefColumnId('child-col').withRefColumnId('some-col'))
      .build();

    expect(() => ERD_VALIDATOR.createRelationship(database, 'schema-1', 'child-table', invalidRelationship)).toThrow(
      RelationshipTargetTableNotExistError
    );
  });

  describe('순환 참조 검증', () => {
    test.skip('IDENTIFYING 관계에서 직접적인 순환 참조는 금지된다', () => {
      // Step 1: 관계 없는 기본 테이블들 생성
      const db = createTestDatabase()
        .withSchema((s) =>
          s
            .withId('schema-1')
            .withTable((t) =>
              t
                .withId('table-a')
                .withName('table_a')
                .withColumn((c) => c.withId('a-id').withName('id').withDataType('INT'))
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
                .withColumn((c) => c.withId('b-id').withName('id').withDataType('INT'))
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
        .withName('fk_a_to_b')
        .withKind('IDENTIFYING') // IDENTIFYING 관계
        .withTgtTableId('table-b')
        .withColumn((rc) => rc.withRefColumnId('b-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', 'table-a', aToBRelationship)).not.toThrow();

      // Step 3: IDENTIFYING 관계에서 순환 참조 시도 (B -> A) - 논리적으로 불가능
      const cyclicRelationship = createRelationshipBuilder()
        .withName('fk_b_to_a')
        .withKind('IDENTIFYING') // IDENTIFYING 관계에서 순환은 불가능
        .withTgtTableId('table-a')
        .withColumn((rc) => rc.withRefColumnId('a-id'))
        .build();

      // IDENTIFYING 관계에서 순환 참조는 금지되어야 함
      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', 'table-b', cyclicRelationship)).toThrow(
        RelationshipCyclicReferenceError
      );
    });

    test.skip('NON_IDENTIFYING 관계에서는 순환 참조가 허용된다', () => {
      // Step 1: 관계 없는 기본 테이블들 생성
      const db = createTestDatabase()
        .withSchema((s) =>
          s
            .withId('schema-1')
            .withTable((t) =>
              t
                .withId('user-table')
                .withName('user')
                .withColumn((c) => c.withId('user-id').withName('id').withDataType('INT'))
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
                .withColumn((c) => c.withId('company-id').withName('id').withDataType('INT'))
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
        .withName('fk_user_company')
        .withKind('NON_IDENTIFYING') // NON_IDENTIFYING 관계
        .withTgtTableId('company-table')
        .withColumn((rc) => rc.withRefColumnId('company-id'))
        .build();

      expect(() =>
        ERD_VALIDATOR.createRelationship(db, 'schema-1', 'user-table', userCompanyRelationship)
      ).not.toThrow();

      // Step 3: NON_IDENTIFYING 관계에서 순환 참조 (company -> user) - 허용되어야 함
      const cyclicRelationship = createRelationshipBuilder()
        .withName('fk_company_owner')
        .withKind('NON_IDENTIFYING') // NON_IDENTIFYING에서는 순환 허용
        .withTgtTableId('user-table')
        .withColumn((rc) => rc.withRefColumnId('user-id'))
        .build();

      // NON_IDENTIFYING 관계에서 순환 참조는 허용되어야 함
      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', 'company-table', cyclicRelationship)).not.toThrow();
    });

    test.skip('테이블이 자기 자신을 참조하는 관계 추가는 허용한다', () => {
      // Step 1: 기본 테이블 생성 (외래키 컬럼 없이)
      const db = createTestDatabase()
        .withSchema((s) =>
          s.withId('schema-1').withTable((t) =>
            t
              .withId('employee-table')
              .withName('employee')
              .withColumn((c) => c.withId('emp-id').withName('id').withDataType('INT'))
              .withConstraint((c) =>
                c
                  .withName('pk_employee')
                  .withKind('PRIMARY_KEY')
                  .withColumn((cc) => cc.withColumnId('emp-id'))
              )
          )
        )
        .build();

      // Step 2: 자기 참조 관계 추가 (manager_id 컬럼이 자동으로 추가될 것임)
      const selfReferenceRelationship = createRelationshipBuilder()
        .withName('fk_employee_manager')
        .withKind('NON_IDENTIFYING')
        .withTgtTableId('employee-table') // 자기 자신을 참조
        .withColumn((rc) => rc.withRefColumnId('emp-id'))
        .build();

      // 자기 참조는 허용되어야 함
      expect(() =>
        ERD_VALIDATOR.createRelationship(db, 'schema-1', 'employee-table', selfReferenceRelationship)
      ).not.toThrow();
    });

    test.skip('IDENTIFYING 관계에서 복합 PK 전파가 올바르게 처리된다', () => {
      // Step 1: 3단계 계층구조 테이블 생성 (Order -> OrderLine -> OrderLineDetail)
      const db = createTestDatabase()
        .withSchema((s) =>
          s
            .withId('schema-1')
            .withTable((t) =>
              t
                .withId('order-table')
                .withName('order')
                .withColumn((c) => c.withId('order-id').withName('id').withDataType('INT'))
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
        .withName('fk_order_line')
        .withKind('IDENTIFYING')
        .withTgtTableId('order-table')
        .withColumn((rc) => rc.withRefColumnId('order-id'))
        .build();

      expect(() =>
        ERD_VALIDATOR.createRelationship(db, 'schema-1', 'order-line-table', orderLineRelationship)
      ).not.toThrow();

      // Step 3: IDENTIFYING 관계 추가 (OrderLine -> OrderLineDetail)
      // 문제: OrderLine의 PK는 이제 복합키 (line_id, order_id)
      // 따라서 OrderLineDetail은 이 복합키 전체를 참조해야 함
      const orderLineDetailRelationship = createRelationshipBuilder()
        .withName('fk_order_line_detail')
        .withKind('IDENTIFYING')
        .withTgtTableId('order-line-table')
        // 복합 PK 참조 - 여러 컬럼이 필요함
        .withColumn((rc) => rc.withRefColumnId('line-id'))
        .withColumn((rc) => rc.withRefColumnId('order-id')) // 전파된 PK도 포함, columnId는 상위 테이블의 PK 컬럼 ID, 내부적으로 관리하는 ID는 자동 생성
        .build();

      // 복합 PK를 올바르게 참조하는 IDENTIFYING 관계는 허용되어야 함
      expect(() =>
        ERD_VALIDATOR.createRelationship(db, 'schema-1', 'order-line-detail-table', orderLineDetailRelationship)
      ).not.toThrow();

      const orderLineDetailTable = db.schemas[0].tables.find((t) => t.id === 'order-line-detail-table');
      expect(orderLineDetailTable).toBeDefined();
      if (orderLineDetailTable) {
        const pkConstraint = orderLineDetailTable.constraints.filter((c) => c.kind === 'PRIMARY_KEY');
        expect(pkConstraint.length).toBe(3); // detail_id, line_id, order_id
      }
    });

    test.skip('에지 케이스: 동일한 테이블 쌍 간에 여러 관계가 있을 때 순환 검증', () => {
      // Step 1: 기본 테이블 생성 (외래키 컬럼들 없이)
      const db = createTestDatabase()
        .withSchema((s) =>
          s.withId('schema-1').withTable((t) =>
            t
              .withId('user-table')
              .withName('user')
              .withColumn((c) => c.withId('user-id').withName('id').withDataType('INT'))
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
        .withName('fk_user_created_by')
        .withKind('NON_IDENTIFYING')
        .withTgtTableId('user-table')
        .withColumn((rc) => rc.withRefColumnId('user-id'))
        .build();

      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', 'user-table', createdByRelationship)).not.toThrow();

      // Step 3: 두 번째 자기 참조 관계 추가 (updated_by_user_id 컬럼이 추가됨)
      const updatedByRelationship = createRelationshipBuilder()
        .withName('fk_user_updated_by')
        .withKind('NON_IDENTIFYING')
        .withTgtTableId('user-table')
        .withColumn((rc) => rc.withRefColumnId('user-id'))
        .build();

      // 같은 테이블에 대한 여러 자기 참조 관계는 허용되어야 함
      expect(() => ERD_VALIDATOR.createRelationship(db, 'schema-1', 'user-table', updatedByRelationship)).not.toThrow();
    });
  });
});
