import {
  normalizeOnUpdate,
  toZodConstraintKind,
  toZodDbVendor,
  toZodIndexSortDir,
  toZodIndexType,
  toZodOnDelete,
  toZodRelationshipCardinality,
  toZodRelationshipKind,
} from './enum-transformers';

describe('enum-transformers', () => {
  describe('toZodDbVendor', () => {
    it('should convert to lowercase', () => {
      expect(toZodDbVendor('MYSQL')).toBe('mysql');
      expect(toZodDbVendor('mysql')).toBe('mysql');
    });

    it('should throw error for unsupported vendor', () => {
      expect(() => toZodDbVendor('postgresql')).toThrow('Unsupported DbVendor');
    });
  });

  describe('toZodIndexType', () => {
    it('should accept valid types', () => {
      expect(toZodIndexType('BTREE')).toBe('BTREE');
      expect(toZodIndexType('HASH')).toBe('HASH');
    });

    it('should throw error for invalid type', () => {
      expect(() => toZodIndexType('INVALID')).toThrow('Unsupported IndexType');
    });
  });

  describe('toZodIndexSortDir', () => {
    it('should accept valid directions', () => {
      expect(toZodIndexSortDir('ASC')).toBe('ASC');
      expect(toZodIndexSortDir('DESC')).toBe('DESC');
    });

    it('should throw error for invalid direction', () => {
      expect(() => toZodIndexSortDir('INVALID')).toThrow(
        'Unsupported IndexSortDir',
      );
    });
  });

  describe('toZodConstraintKind', () => {
    it('should accept valid kinds', () => {
      expect(toZodConstraintKind('PRIMARY_KEY')).toBe('PRIMARY_KEY');
      expect(toZodConstraintKind('UNIQUE')).toBe('UNIQUE');
    });

    it('should throw error for invalid kind', () => {
      expect(() => toZodConstraintKind('FOREIGN_KEY')).toThrow(
        'Unsupported ConstraintKind',
      );
    });
  });

  describe('toZodRelationshipKind', () => {
    it('should accept valid kinds', () => {
      expect(toZodRelationshipKind('IDENTIFYING')).toBe('IDENTIFYING');
      expect(toZodRelationshipKind('NON_IDENTIFYING')).toBe('NON_IDENTIFYING');
    });

    it('should throw error for invalid kind', () => {
      expect(() => toZodRelationshipKind('INVALID')).toThrow(
        'Unsupported RelationshipKind',
      );
    });
  });

  describe('toZodOnDelete', () => {
    it('should accept valid actions', () => {
      expect(toZodOnDelete('CASCADE')).toBe('CASCADE');
      expect(toZodOnDelete('SET_NULL')).toBe('SET_NULL');
    });

    it('should throw error for invalid action', () => {
      expect(() => toZodOnDelete('INVALID')).toThrow('Unsupported OnAction');
    });
  });

  describe('normalizeOnUpdate', () => {
    it('should remove _UPDATE suffix', () => {
      expect(normalizeOnUpdate('CASCADE_UPDATE')).toBe('CASCADE');
      expect(normalizeOnUpdate('SET_NULL_UPDATE')).toBe('SET_NULL');
    });

    it('should throw error for invalid action', () => {
      expect(() => normalizeOnUpdate('INVALID_UPDATE')).toThrow(
        'Unsupported OnAction',
      );
    });
  });

  describe('toZodRelationshipCardinality', () => {
    it('should convert proto format to zod format', () => {
      expect(toZodRelationshipCardinality('ONE_TO_ONE')).toBe('1:1');
      expect(toZodRelationshipCardinality('ONE_TO_MANY')).toBe('1:N');
    });

    it('should throw error for invalid cardinality', () => {
      expect(() => toZodRelationshipCardinality('MANY_TO_MANY')).toThrow(
        'Unsupported RelationshipCardinality',
      );
    });
  });
});
