import "./globals.css";

import { BzUiRoot } from "@admin/components/bz";
import type { Metadata } from "next";
import { Inter } from "next/font/google";

const inter = Inter({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-inter",
});

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
    <html lang="zh-CN" className={inter.variable}>
      <body>
        <BzUiRoot>{children}</BzUiRoot>
      </body>
    </html>
  );
}
