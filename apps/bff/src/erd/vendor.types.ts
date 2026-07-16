export type DbVendorSummaryResponse = {
  id: string;
  displayName: string;
  name: string;
  version: string;
};

export type DbVendorDetailResponse = {
  id: string;
  displayName: string;
  name: string;
  version: string;
  datatypeMappings: unknown;
};
