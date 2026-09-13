import { useQuery } from '@tanstack/react-query';
import { getVendor, listVendors } from '../api/vendor.api';

export const useVendors = () => {
  return useQuery({
    queryKey: ['vendors'],
    queryFn: listVendors,
    staleTime: Infinity,
  });
};

export const useVendor = (id: number) => {
  return useQuery({
    queryKey: ['vendors', id],
    queryFn: () => getVendor(id),
    enabled: id > 0,
    staleTime: Infinity,
  });
};
