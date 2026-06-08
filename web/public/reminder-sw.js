self.addEventListener("push", (event) => {
  console.info("[reminder-sw] push event received");
  event.waitUntil(
    (async () => {
      const payload = await parsePushPayload(event.data);
      console.info("[reminder-sw] parsed push payload", {
        eventId: payload.eventId,
        notificationId: payload.notificationId,
        msgType: payload.msgType,
        priority: payload.priority,
        route: payload.route,
      });
      const clientsList = await self.clients.matchAll({
        type: "window",
        includeUncontrolled: true,
      });

      const hasVisibleClient = clientsList.some((client) => client.visibilityState === "visible");
      clientsList.forEach((client) => {
        client.postMessage({
          type: "WEB_PUSH_MESSAGE",
          payload,
        });
      });
      console.info("[reminder-sw] posted message to clients", {
        clientCount: clientsList.length,
      });

      const shouldForceSystemNotification =
        payload.osNotificationEnabled || payload.priority === "HIGH";

      if (hasVisibleClient && !shouldForceSystemNotification) {
        console.info("[reminder-sw] skip system notification because client is visible");
        return;
      }

      await self.registration.showNotification(payload.title, {
        body: payload.content,
        tag: payload.eventId ? `msg-${payload.eventId}` : undefined,
        data: { path: payload.route || "/todo/all" },
        icon: "/icon-512.png",
        badge: "/icon-512.png",
      });
      console.info("[reminder-sw] system notification displayed");
    })(),
  );
});

self.addEventListener("notificationclick", (event) => {
  console.info("[reminder-sw] notification click received");
  event.notification.close();
  const targetPath = event.notification?.data?.path || "/todo/all";

  event.waitUntil(
    (async () => {
      const clientsList = await self.clients.matchAll({
        type: "window",
        includeUncontrolled: true,
      });
      for (const client of clientsList) {
        if ("focus" in client) {
          await client.navigate(targetPath);
          await client.focus();
          return;
        }
      }
      if (self.clients.openWindow) {
        await self.clients.openWindow(targetPath);
      }
    })(),
  );
});

async function parsePushPayload(data) {
  if (!data) {
    return {
      eventId: "",
      notificationId: "",
      msgType: "SYSTEM_EVENT",
      title: "消息提醒",
      content: "您有一条新消息",
      route: "/todo/all",
      priority: "MEDIUM",
      panelAutoOpen: true,
      osNotificationEnabled: false,
    };
  }

  try {
    const parsed = data.json();
    return {
      eventId: parsed?.eventId || "",
      notificationId: parsed?.notificationId || "",
      msgType: parsed?.msgType || "SYSTEM_EVENT",
      title: parsed?.title || "消息提醒",
      content: parsed?.content || "您有一条新消息",
      route: parsed?.route || "/todo/all",
      priority: parsed?.priority || "MEDIUM",
      panelAutoOpen: resolveBoolean(
        parsed?.panelAutoOpen,
        (parsed?.priority || "MEDIUM") !== "LOW",
      ),
      osNotificationEnabled: resolveBoolean(
        parsed?.osNotificationEnabled,
        (parsed?.priority || "MEDIUM") === "HIGH",
      ),
    };
  } catch (_err) {
    let text = "您有一条新消息";
    if (typeof data.text === "function") {
      try {
        text = await data.text();
      } catch (_readError) {
        text = "您有一条新消息";
      }
    }
    return {
      eventId: "",
      notificationId: "",
      msgType: "SYSTEM_EVENT",
      title: "消息提醒",
      content: text,
      route: "/todo/all",
      priority: "MEDIUM",
      panelAutoOpen: true,
      osNotificationEnabled: false,
    };
  }
}

function resolveBoolean(raw, fallback) {
  if (typeof raw === "boolean") {
    return raw;
  }
  if (typeof raw === "string") {
    const normalized = raw.trim().toLowerCase();
    if (normalized === "true") {
      return true;
    }
    if (normalized === "false") {
      return false;
    }
  }
  return fallback;
}
