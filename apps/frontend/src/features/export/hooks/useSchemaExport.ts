import { useEffect, useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getApiErrorMessage } from '@/lib/api';
import {
  exportSchema,
  type SchemaExportRequest,
  type SchemaExportResult,
} from '../api';

export type SchemaExportState =
  | { status: 'loading'; request: SchemaExportRequest }
  | { status: 'error'; request: SchemaExportRequest; errorMessage: string }
  | {
      status: 'success';
      request: SchemaExportRequest;
      result: SchemaExportResult;
    };

export const useSchemaExport = () => {
  const [request, setRequest] = useState<SchemaExportRequest | null>(null);
  const controllerRef = useRef<AbortController | null>(null);
  const exportMutation = useMutation({ mutationFn: exportSchema });

  useEffect(
    () => () => {
      controllerRef.current?.abort();
      controllerRef.current = null;
    },
    [],
  );

  const startExport = (exportRequest: SchemaExportRequest) => {
    if (exportMutation.isPending) return;

    const controller = new AbortController();
    controllerRef.current?.abort();
    controllerRef.current = controller;
    setRequest(exportRequest);
    exportMutation.mutate({
      schemaId: exportRequest.schemaId,
      format: exportRequest.format,
      signal: controller.signal,
    });
  };

  const retryExport = () => {
    if (request) startExport(request);
  };

  const closeExport = () => {
    controllerRef.current?.abort();
    controllerRef.current = null;
    exportMutation.reset();
    setRequest(null);
  };

  let state: SchemaExportState | null = null;
  if (request) {
    if (exportMutation.isError) {
      state = {
        status: 'error',
        request,
        errorMessage: getApiErrorMessage(exportMutation.error),
      };
    } else if (exportMutation.isSuccess) {
      state = { status: 'success', request, result: exportMutation.data };
    } else {
      state = { status: 'loading', request };
    }
  }

  return {
    state,
    isPending: exportMutation.isPending,
    startExport,
    retryExport,
    closeExport,
  };
};
