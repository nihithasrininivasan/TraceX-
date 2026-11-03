export function validateReport(body) {
  if (body.type === "Other" && !body.customType?.trim())
    return "Please specify the incident type.";
  if (!body.title?.trim()) return "Enter a short incident title.";
  if ((body.description?.trim().length || 0) < 15)
    return "Describe the incident in at least 15 characters.";
  if (body.url) {
    try {
      const parsed = new URL(body.url);
      if (!["http:", "https:"].includes(parsed.protocol))
        return "Use an HTTP or HTTPS URL.";
    } catch {
      return "Enter a valid suspicious URL.";
    }
  }
  if (body.ip && !/^(\d{1,3}\.){3}\d{1,3}$/.test(body.ip))
    return "Enter a valid IPv4 address.";
  if (body.ip && body.ip.split(".").some((part) => Number(part) > 255))
    return "IP address numbers must be between 0 and 255.";
  if (body.evidence) {
    if (body.evidence.size > 5 * 1024 * 1024)
      return "Choose evidence smaller than 5 MB.";
    if (
      !["image/png", "image/jpeg", "application/pdf"].includes(
        body.evidence.type,
      )
    )
      return "Choose PNG, JPG, or PDF evidence.";
  }
  return "";
}
