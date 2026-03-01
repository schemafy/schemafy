import { useQuery } from '@tanstack/react-query';
import { listVendors, getVendor } from '../api/vendor.api';

export const useVendors = () => {
  return useQuery({
    queryKey: ['vendors'],
    queryFn: listVendors,
    staleTime: Infinity,
  });
};

export const useVendor = (displayName: string) => {
  return useQuery({
    queryKey: ['vendors', displayName],
    queryFn: () => getVendor(displayName),
    enabled: !!displayName,
    staleTime: Infinity,
  });
};
