export function readUser() {
  try {
    const user = JSON.parse(sessionStorage.getItem("tracex-user"));
    return typeof user?.name === "string" &&
      user.name.trim() &&
      ["STUDENT", "ADMIN"].includes(user.role)
      ? user
      : null;
  } catch {
    return null;
  }
}
