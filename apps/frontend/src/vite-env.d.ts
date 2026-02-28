/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_BASE_URL: string;
  readonly VITE_WS_URL: string;
  readonly VITE_PUBLIC_BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
