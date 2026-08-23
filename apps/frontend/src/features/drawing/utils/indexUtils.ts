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

// Edit mode offers a control the vendor may reject, so it must wait until the
// vendor's real answer is known before allowing sort direction to be edited.
export const canEditSortDirection = (
  capabilities: IndexCapabilities,
  type: IndexType,
): boolean =>
  capabilities.status === 'ready' &&
  capabilities.sortDirectionTypes.includes(type);

// View mode only displays the index's already-stored sort direction, so it has
// nothing to wait on — show it unless the vendor is positively known not to
// use it for this type.
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
