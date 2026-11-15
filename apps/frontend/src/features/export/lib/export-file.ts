import type { ExportFormat } from '../api';

const INVALID_FILE_NAME_CHARACTERS = new Set([
  '<',
  '>',
  ':',
  '"',
  '/',
  '\\',
  '|',
  '?',
  '*',
]);
const TRAILING_DOTS_OR_SPACES = /[. ]+$/g;

const getExtension = (format: ExportFormat) =>
  format === 'ddl' ? '.sql' : '.mmd';

export const sanitizeExportFileName = (schemaName: string): string => {
  const sanitizedName = Array.from(schemaName, (character) => {
    const codePoint = character.codePointAt(0) ?? 0;
    const isControlCharacter = codePoint <= 31 || codePoint === 127;

    return INVALID_FILE_NAME_CHARACTERS.has(character) || isControlCharacter
      ? '-'
      : character;
  })
    .join('')
    .replace(TRAILING_DOTS_OR_SPACES, '-');

  return sanitizedName || 'schema';
};

interface DownloadExportFileOptions {
  code: string;
  format: ExportFormat;
  schemaName: string;
}

export const downloadExportFile = ({
  code,
  format,
  schemaName,
}: DownloadExportFileOptions) => {
  const blob = new Blob([code], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');

  try {
    anchor.href = url;
    anchor.download = `${sanitizeExportFileName(schemaName)}${getExtension(format)}`;
    anchor.hidden = true;
    document.body.appendChild(anchor);
    anchor.click();
  } finally {
    anchor.remove();
    URL.revokeObjectURL(url);
  }
};
