import "./globals.css";

import { BzUiRoot } from "@admin/components/bz";
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
        <BzUiRoot>{children}</BzUiRoot>
      </body>
    </html>
  );
}
