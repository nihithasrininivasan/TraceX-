// Keep these rules aligned with backend ContactFormats.
const emailFormat =
  /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$/;
export function isValidEmail(value) {
  return (
    typeof value === "string" &&
    value.length <= 254 &&
    value.indexOf("@") <= 64 &&
    emailFormat.test(value)
  );
}
export function isValidSender(value) {
  const text = (value || "").trim();
  if (!text || isValidEmail(text)) return true;
  if (!/^\+?[0-9]+(?:[ -]?(?:[0-9]+|\([0-9]{2,5}\)))*$/.test(text))
    return false;
  const digits = text.replace(/[^0-9]/g, "");
  if (digits.startsWith("0")) return false;
  return text.startsWith("+")
    ? digits.length >= 8 && digits.length <= 15
    : digits.length === 10;
}

export function validateReport(body) {
  if (!isValidSender(body.sender))
    return "Enter a valid sender email or phone number: 10 digits, or + country code and 8–15 digits.";
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
