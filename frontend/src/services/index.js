import { authService } from "./authService";
import { incidentService } from "./incidentService";
export { api, demo } from "./api";
export { clearCredentials } from "./api";
export { authService, incidentService };
export const service = { ...authService, ...incidentService };
