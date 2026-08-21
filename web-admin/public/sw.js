self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (event) => event.waitUntil(self.clients.claim()));

self.addEventListener("push", (event) => {
  const payload = readPayload(event);
  if (!payload) return;

  const message = {
    type: "BREEZY_WEB_PUSH_MESSAGE",
    eventId: stringValue(payload.eventId),
    notificationId: stringValue(payload.notificationId),
    msgType: stringValue(payload.msgType),
    title: stringValue(payload.title) || "Breezy",
    content: stringValue(payload.content || payload.body),
    route: normalizeRoute(payload.route),
    priority: stringValue(payload.priority) || "LOW",
    panelAutoOpen: payload.panelAutoOpen === true,
    osNotificationEnabled: payload.osNotificationEnabled === true,
  };

  event.waitUntil(
    Promise.all([
      broadcast(message),
      message.osNotificationEnabled
        ? self.registration.showNotification(message.title, {
            body: message.content,
            data: { route: message.route },
            tag: message.notificationId || message.eventId || undefined,
          })
        : Promise.resolve(),
    ]),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const route = normalizeRoute(event.notification.data?.route) || "/admin";
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      const existing = clients.find(
        (client) => new URL(client.url).origin === self.location.origin,
      );
      if (existing) {
        existing.navigate(route);
        return existing.focus();
      }
      return self.clients.openWindow(route);
    }),
  );
});

function readPayload(event) {
  if (!event.data) return null;
  try {
    const value = event.data.json();
    return value && typeof value === "object" ? value : null;
  } catch {
    return null;
  }
}

function broadcast(message) {
  return self.clients
    .matchAll({ type: "window", includeUncontrolled: true })
    .then((clients) => Promise.all(clients.map((client) => client.postMessage(message))));
}

function stringValue(value) {
  return typeof value === "string" || typeof value === "number" ? String(value).trim() : "";
}

function normalizeRoute(value) {
  const route = stringValue(value);
  if (!route) return "";
  try {
    const url = new URL(route.startsWith("/") ? route : `/${route}`, self.location.origin);
    if (url.origin !== self.location.origin) return "";
    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return "";
  }
}
