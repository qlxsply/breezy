import { PHASE_DEVELOPMENT_SERVER } from "next/constants";
import type { NextConfig } from "next";

export default function nextConfig(phase: string): NextConfig {
  return {
    reactStrictMode: true,
    output: "export",
    ...(phase === PHASE_DEVELOPMENT_SERVER ? {} : { distDir: "dist" }),
    images: {
      unoptimized: true,
    },
  };
}
