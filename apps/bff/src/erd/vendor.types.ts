export type DbVendorSummaryResponse = {
  id: number;
  displayName: string;
  name: string;
  version: string;
};

export type DbVendorDetailResponse = {
  id: number;
  displayName: string;
  name: string;
  version: string;
  datatypeMappings: unknown;
};
