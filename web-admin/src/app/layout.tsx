import "@fontsource-variable/inter/wght.css";
import "@fontsource-variable/jetbrains-mono/wght.css";
import "./globals.css";

import { RuntimeConfigGate } from "@admin/runtime/config/RuntimeConfigGate";
import { BzFeedbackBridge } from "@admin/runtime/feedback/BzFeedbackBridge";
import { SessionLifecycleBridge } from "@admin/runtime/session/SessionLifecycleBridge";
import { BzUiRoot } from "@admin/shared/ui/bz";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Breezy Admin",
  description: "Breezy web admin",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body>
        <RuntimeConfigGate>
          <SessionLifecycleBridge />
          <BzUiRoot>
            <BzFeedbackBridge />
            {children}
          </BzUiRoot>
        </RuntimeConfigGate>
      </body>
    </html>
  );
}
