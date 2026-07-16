import { useQuery } from '@tanstack/react-query';
import { getVendor, listVendors } from '../api/vendor.api';

export const useVendors = () => {
  return useQuery({
    queryKey: ['vendors'],
    queryFn: listVendors,
    staleTime: Infinity,
  });
};

export const useVendor = (id: string) => {
  return useQuery({
    queryKey: ['vendors', id],
    queryFn: () => getVendor(id),
    enabled: !!id,
    staleTime: Infinity,
  });
};
