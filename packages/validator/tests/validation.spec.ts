import { z } from 'zod';
import { VALIDATOR_VERSION } from '../src';

describe('validator package basics', () => {
  test('zod works and validates simple object', () => {
    const schema = z.object({ name: z.string() });
    const result = schema.safeParse({ name: 'hello' });
    expect(result.success).toBe(true);
  });

  test('exposes version constant', () => {
    expect(VALIDATOR_VERSION).toMatch(/\d+\.\d+\.\d+/);
  });
});
