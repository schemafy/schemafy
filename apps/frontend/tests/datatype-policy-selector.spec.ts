import { expect, test } from '@playwright/test';
import type { DatatypeParameter } from '../src/features/vendor/api/types';
import {
  applyDatatypeParameterInput,
  editDatatypeParameterDraft,
  formatTypeDisplay,
  hasCompleteRequiredParameters,
  serializeDatatypeParameterValues,
} from '../src/features/drawing/components/Column/utils';

const integerParameter = (
  overrides: Partial<DatatypeParameter> = {},
): DatatypeParameter => ({
  name: 'length',
  label: 'Length',
  valueType: 'integer',
  required: true,
  order: 1,
  minValue: 0,
  maxValue: 65_535,
  minItems: null,
  maxItems: null,
  minItemLength: null,
  maxItemLength: null,
  ...overrides,
});

test.describe('datatype policy selector state', () => {
  test('required VARCHAR length와 optional CHAR length의 0 및 bounds를 구분한다', () => {
    const varcharLength = integerParameter();
    const charLength = integerParameter({ required: false, maxValue: 255 });

    const emptyRequired = applyDatatypeParameterInput(
      { values: {}, invalidNames: new Set() },
      varcharLength,
      '',
    );
    expect(emptyRequired.invalidNames).toContain('length');
    expect(hasCompleteRequiredParameters([varcharLength], emptyRequired)).toBe(
      false,
    );

    const zeroRequired = applyDatatypeParameterInput(
      emptyRequired,
      varcharLength,
      '0',
    );
    expect(zeroRequired.values).toEqual({ length: 0 });
    expect(hasCompleteRequiredParameters([varcharLength], zeroRequired)).toBe(
      true,
    );

    const emptyOptional = applyDatatypeParameterInput(
      { values: { length: 10 }, invalidNames: new Set() },
      charLength,
      '',
    );
    expect(emptyOptional.values).toEqual({});
    expect(emptyOptional.invalidNames.size).toBe(0);

    const outOfRange = applyDatatypeParameterInput(
      zeroRequired,
      varcharLength,
      '65536',
    );
    expect(outOfRange.values).toEqual({ length: 0 });
    expect(outOfRange.invalidNames).toContain('length');
    expect(hasCompleteRequiredParameters([varcharLength], outOfRange)).toBe(
      false,
    );
  });

  test('ENUM item count와 Unicode code-point item length 위반을 pending으로 유지한다', () => {
    const valuesParameter: DatatypeParameter = {
      name: 'values',
      label: 'Values',
      valueType: 'string_array',
      required: true,
      order: 1,
      minValue: null,
      maxValue: null,
      minItems: 1,
      maxItems: 2,
      minItemLength: 1,
      maxItemLength: 2,
    };
    const initial = { values: {}, invalidNames: new Set<string>() };

    const tooMany = applyDatatypeParameterInput(
      initial,
      valuesParameter,
      'A, B, C',
    );
    expect(tooMany.values).toEqual({});
    expect(tooMany.invalidNames).toContain('values');

    const tooLong = applyDatatypeParameterInput(
      tooMany,
      valuesParameter,
      '가나다',
    );
    expect(tooLong.values).toEqual({});
    expect(tooLong.invalidNames).toContain('values');

    const valid = applyDatatypeParameterInput(
      tooLong,
      valuesParameter,
      '😀, A',
    );
    expect(valid.values).toEqual({ values: ['😀', 'A'] });
    expect(valid.invalidNames.size).toBe(0);
    expect(hasCompleteRequiredParameters([valuesParameter], valid)).toBe(true);
    expect(serializeDatatypeParameterValues(valid.values)).toBe(
      JSON.stringify({ values: ['😀', 'A'] }),
    );
  });

  test('canonical mutation payload에는 null이 아닌 policy parameter만 포함한다', () => {
    expect(
      serializeDatatypeParameterValues({
        length: null,
        precision: 10,
        scale: null,
        values: null,
      }),
    ).toBe(JSON.stringify({ precision: 10 }));
  });

  test('기존 parameter의 invalid edit은 수정될 때까지 pending으로 유지한다', () => {
    const length = integerParameter({ required: false, maxValue: 255 });

    const invalid = editDatatypeParameterDraft(
      { values: { length: 100 }, invalidNames: new Set() },
      [length],
      length,
      '256',
    );
    expect(invalid.isPending).toBe(true);
    expect(invalid.state.values).toEqual({ length: 100 });
    expect(invalid.state.invalidNames).toContain('length');

    const corrected = editDatatypeParameterDraft(
      invalid.state,
      [length],
      length,
      '200',
    );
    expect(corrected.isPending).toBe(false);
    expect(corrected.state.values).toEqual({ length: 200 });
    expect(corrected.state.invalidNames.size).toBe(0);
  });

  test('optional DECIMAL parameter edit은 하나의 draft에 누적한다', () => {
    const precision = integerParameter({
      name: 'precision',
      label: 'Precision (M)',
      required: false,
      order: 1,
      minValue: 1,
      maxValue: 65,
    });
    const scale = integerParameter({
      name: 'scale',
      label: 'Scale (D)',
      required: false,
      order: 2,
      minValue: 0,
      maxValue: 30,
    });

    const precisionEdit = editDatatypeParameterDraft(
      { values: {}, invalidNames: new Set() },
      [precision, scale],
      precision,
      '10',
    );
    const scaleEdit = editDatatypeParameterDraft(
      precisionEdit.state,
      [precision, scale],
      scale,
      '2',
    );

    expect(precisionEdit.isPending).toBe(false);
    expect(scaleEdit.isPending).toBe(false);
    expect(scaleEdit.state.values).toEqual({ precision: 10, scale: 2 });
    expect(serializeDatatypeParameterValues(scaleEdit.state.values)).toBe(
      JSON.stringify({ precision: 10, scale: 2 }),
    );
  });

  test('DECIMAL scale-only와 scale이 precision을 초과한 draft는 pending으로 유지한다', () => {
    const precision = integerParameter({
      name: 'precision',
      label: 'Precision (M)',
      required: false,
      order: 1,
      minValue: 1,
      maxValue: 65,
    });
    const scale = integerParameter({
      name: 'scale',
      label: 'Scale (D)',
      required: false,
      order: 2,
      minValue: 0,
      maxValue: 30,
    });

    const scaleOnly = editDatatypeParameterDraft(
      { values: {}, invalidNames: new Set() },
      [precision, scale],
      scale,
      '2',
    );
    const scaleExceedsPrecision = editDatatypeParameterDraft(
      scaleOnly.state,
      [precision, scale],
      precision,
      '1',
    );
    const corrected = editDatatypeParameterDraft(
      scaleExceedsPrecision.state,
      [precision, scale],
      precision,
      '10',
    );

    expect(scaleOnly.isPending).toBe(true);
    expect(scaleExceedsPrecision.isPending).toBe(true);
    expect(corrected.isPending).toBe(false);
  });

  test('ENUM과 SET의 중복 값은 pending으로 유지한다', () => {
    const values = {
      name: 'values',
      label: 'Values',
      valueType: 'string_array',
      required: true,
      order: 1,
      minValue: null,
      maxValue: null,
      minItems: 1,
      maxItems: 64,
      minItemLength: 1,
      maxItemLength: 255,
    } satisfies DatatypeParameter;

    const duplicate = editDatatypeParameterDraft(
      { values: {}, invalidNames: new Set() },
      [values],
      values,
      'ACTIVE, ACTIVE',
    );

    expect(duplicate.isPending).toBe(true);
    expect(duplicate.state.invalidNames).toContain('values');
    expect(duplicate.state.values).toEqual({});
  });

  test('scale 없는 DECIMAL precision을 view mode에 표시한다', () => {
    expect(
      formatTypeDisplay('DECIMAL', JSON.stringify({ precision: 10 })),
    ).toBe('DECIMAL(10)');
  });
});
