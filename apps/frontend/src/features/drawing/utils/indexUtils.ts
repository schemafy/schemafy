import type { IndexCapabilities, IndexType } from '../types';

export const getDefaultIndexType = (
  capabilities: IndexCapabilities,
): IndexType => {
  if (capabilities.supportedTypes.includes('BTREE')) {
    return 'BTREE';
  }
  return capabilities.supportedTypes[0] ?? 'BTREE';
};

export const canSelectIndexType = (capabilities: IndexCapabilities): boolean =>
  capabilities.status === 'ready' && capabilities.supportedTypes.length > 0;

export const canEditSortDirection = (
  capabilities: IndexCapabilities,
  type: IndexType,
): boolean =>
  capabilities.status === 'ready' &&
  capabilities.sortDirectionTypes.includes(type);

export const canDisplaySortDirection = (
  capabilities: IndexCapabilities,
  type: IndexType,
): boolean =>
  capabilities.status !== 'ready' ||
  capabilities.sortDirectionTypes.includes(type);

export const getCapabilitiesUnavailableMessage = (
  capabilities: IndexCapabilities,
): string => {
  switch (capabilities.status) {
    case 'loading':
      return 'Loading vendor capabilities…';
    case 'error':
      return 'Unable to load vendor capabilities.';
    case 'ready':
      return 'This vendor does not support any index types.';
    default: {
      const exhaustive: never = capabilities.status;
      return exhaustive;
    }
  }
};
