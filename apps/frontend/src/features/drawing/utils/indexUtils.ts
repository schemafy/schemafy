import type { IndexCapabilities, IndexType } from '../types';

export const getDefaultIndexType = (
  capabilities: IndexCapabilities,
): IndexType => {
  if (capabilities.supportedTypes.includes('BTREE')) {
    return 'BTREE';
  }
  return capabilities.supportedTypes[0] ?? 'BTREE';
};
