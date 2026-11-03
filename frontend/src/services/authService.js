import { api, demo, setCredentials, clearCredentials } from "./api";

export const authService = {
  async login(credentials) {
    clearCredentials();
    if (demo)
      throw new Error(
        "Use a demo access button, or connect the authentication API.",
      );
    const { data } = await api.post("/auth/login", credentials);
    if (!data.name || !["STUDENT", "ADMIN"].includes(data.role)) {
      throw new Error(
        "Unexpected login response. Expected a user with name and role.",
      );
    }
    setCredentials(credentials.email, credentials.password);
    return { user: data };
  },
  async register(details) {
    if (demo)
      throw new Error(
        "Registration requires your team’s authentication API. Explore using demo access.",
      );
    return (await api.post("/auth/register", details)).data;
  },
};
