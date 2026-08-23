import type { IndexCapabilities, IndexType } from '../types';

export const getDefaultIndexType = (
  capabilities: IndexCapabilities,
): IndexType => {
  if (capabilities.supportedTypes.includes('BTREE')) {
    return 'BTREE';
  }
  return capabilities.supportedTypes[0] ?? 'BTREE';
};

export const getCapabilitiesUnavailableMessage = (
  capabilities: IndexCapabilities,
): string => {
  switch (capabilities.status) {
    case 'loading':
      return 'Loading vendor capabilities…';
    case 'error':
      return 'Unable to load vendor capabilities.';
    default:
      return 'This vendor does not support any index types.';
  }
};
