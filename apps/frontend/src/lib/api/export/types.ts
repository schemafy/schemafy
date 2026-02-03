export type ExportFormat = 'ddl' | 'mermaid';

export interface ExportResponse {
  format: ExportFormat;
  schemaName: string;
  content: string;
  tableCount: number;
  exportedAt: string;
}

export const EXPORT_CONFIG: Record<
  ExportFormat,
  { title: string; extension: string }
> = {
  ddl: { title: 'Export DDL', extension: '.sql' },
  mermaid: { title: 'Export Mermaid', extension: '.mmd' },
};
