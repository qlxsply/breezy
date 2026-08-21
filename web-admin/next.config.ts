import type { NextConfig } from "next";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants";

export default function nextConfig(phase: string): NextConfig {
  const development = phase === PHASE_DEVELOPMENT_SERVER;
  const apiUpstream = (process.env.ADMIN_API_UPSTREAM || "http://localhost:8910").replace(
    /\/$/,
    "",
  );
  if (development && !/^https?:\/\/[A-Za-z0-9._:-]+$/.test(apiUpstream)) {
    throw new Error("ADMIN_API_UPSTREAM 必须是只包含 Origin 的 HTTP/HTTPS 地址");
  }

  return {
    reactStrictMode: true,
    output: "export",
    trailingSlash: true,
    ...(development
      ? {
          async rewrites() {
            return [
              {
                source: "/api/:path*",
                destination: `${apiUpstream}/api/:path*`,
              },
            ];
          },
        }
      : { distDir: "dist" }),
    images: {
      unoptimized: true,
    },
  };
}
