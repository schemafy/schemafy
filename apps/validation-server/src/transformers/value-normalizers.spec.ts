import { emptyStringToNull, parseIsoDate } from './value-normalizers';

describe('value-normalizers', () => {
    describe('parseIsoDate', () => {
        it('should parse valid ISO date strings', () => {
            const dateString = '2025-10-08T10:30:00.000Z';
            const result = parseIsoDate(dateString);
            expect(result).toBeInstanceOf(Date);
            expect(result?.toISOString()).toBe(dateString);
        });

        it('should return null for empty values', () => {
            expect(parseIsoDate(null)).toBeNull();
            expect(parseIsoDate(undefined)).toBeNull();
            expect(parseIsoDate('')).toBeNull();
        });

        it('should throw error for invalid date strings', () => {
            expect(() => parseIsoDate('not-a-date')).toThrow('Invalid ISO date');
            expect(() => parseIsoDate('2025-13-45')).toThrow('Invalid ISO date');
        });
    });

    describe('emptyStringToNull', () => {
        it('should convert empty string to null', () => {
            expect(emptyStringToNull('')).toBeNull();
        });

        it('should preserve non-empty strings', () => {
            expect(emptyStringToNull('hello')).toBe('hello');
        });

        it('should preserve null and undefined', () => {
            expect(emptyStringToNull(null)).toBeNull();
            expect(emptyStringToNull(undefined)).toBeUndefined();
        });
    });
});
