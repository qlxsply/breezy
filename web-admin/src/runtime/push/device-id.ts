const PUSH_DEVICE_ID_KEY = "breezy:push:device-id";

export function getPushDeviceId(): string {
  if (typeof window === "undefined") {
    return "";
  }

  const existing = window.localStorage.getItem(PUSH_DEVICE_ID_KEY);
  if (existing && existing.trim()) {
    return existing;
  }

  const generated =
    typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
      ? crypto.randomUUID()
      : `push-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
  window.localStorage.setItem(PUSH_DEVICE_ID_KEY, generated);
  return generated;
}
