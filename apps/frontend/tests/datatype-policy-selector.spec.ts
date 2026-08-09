import { expect, test } from '@playwright/test';
import type { DatatypeParameter } from '../src/features/vendor/api/types';
import {
  applyDatatypeParameterInput,
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
});
