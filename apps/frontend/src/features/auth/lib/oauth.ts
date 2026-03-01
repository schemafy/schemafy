const BASE_URL =
  import.meta.env.VITE_PUBLIC_BASE_URL ||
  'http://localhost:8080/public/api/v1.0';

export const gitHubLogin = () => {
  window.location.href = `${BASE_URL}/oauth/github/authorize`;
};
