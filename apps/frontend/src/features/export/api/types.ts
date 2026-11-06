export type ExportFormat = 'ddl' | 'mermaid';

export interface SchemaDdlExportResponse {
  schemaId: string;
  currentRevision: number;
  targetDbVendor: 'mysql';
  ddl: string;
}

export interface SchemaMermaidExportResponse {
  schemaId: string;
  currentRevision: number;
  mermaid: string;
}

export type SchemaExportResult =
  | { format: 'ddl'; data: SchemaDdlExportResponse }
  | { format: 'mermaid'; data: SchemaMermaidExportResponse };

export interface ExportSchemaVariables {
  schemaId: string;
  format: ExportFormat;
  signal: AbortSignal;
}

export interface SchemaExportRequest {
  schemaId: string;
  schemaName: string;
  format: ExportFormat;
}
