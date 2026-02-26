import { useQuery } from '@tanstack/react-query';
import { listVendors, getVendor } from './vendor.api';

export const useVendors = () => {
  return useQuery({
    queryKey: ['vendors'],
    queryFn: listVendors,
  });
};

export const useVendor = (displayName: string) => {
  return useQuery({
    queryKey: ['vendors', displayName],
    queryFn: () => getVendor(displayName),
    enabled: !!displayName,
  });
};
