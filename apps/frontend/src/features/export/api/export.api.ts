import { apiClient, type RequestConfigWithMeta } from '@/lib/api';
import type {
  ExportSchemaVariables,
  SchemaDdlExportResponse,
  SchemaExportResult,
  SchemaMermaidExportResponse,
} from './types';

const EXPORT_TIMEOUT_MS = 35_000;

export const exportSchema = async ({
  schemaId,
  format,
  signal,
}: ExportSchemaVariables): Promise<SchemaExportResult> => {
  if (format === 'ddl') {
    const config: RequestConfigWithMeta = {
      params: { targetDbVendor: 'mysql' },
      timeout: EXPORT_TIMEOUT_MS,
      signal,
      errorPolicy: 'suppress-toast',
    };
    const { data } = await apiClient.get<SchemaDdlExportResponse>(
      `/schemas/${schemaId}/exports/ddl`,
      config,
    );
    return { format, data };
  }

  const config: RequestConfigWithMeta = {
    timeout: EXPORT_TIMEOUT_MS,
    signal,
    errorPolicy: 'suppress-toast',
  };
  const { data } = await apiClient.get<SchemaMermaidExportResponse>(
    `/schemas/${schemaId}/exports/mermaid`,
    config,
  );
  return { format, data };
};
