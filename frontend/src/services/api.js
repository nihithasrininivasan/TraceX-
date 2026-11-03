import axios from "axios";

export const demo = import.meta.env.VITE_API_MODE !== "live";
let basicCredentials = null;
export function setCredentials(email, password) {
  basicCredentials = btoa(
    Array.from(new TextEncoder().encode(`${email}:${password}`), (byte) =>
      String.fromCharCode(byte),
    ).join(""),
  );
}
export function clearCredentials() {
  basicCredentials = null;
}
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8080/api",
  timeout: 15000,
  adapter: "fetch",
  fetchOptions: { credentials: "omit" },
});

api.interceptors.request.use((config) => {
  if (basicCredentials && !config.url.startsWith("/auth/"))
    config.headers.Authorization = `Basic ${basicCredentials}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearCredentials();
      sessionStorage.removeItem("tracex-token");
      sessionStorage.removeItem("tracex-user");
      window.dispatchEvent(new Event("tracex-session-expired"));
    }
    return Promise.reject(error);
  },
);
