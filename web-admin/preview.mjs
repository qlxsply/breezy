import { createReadStream } from "node:fs";
import { stat } from "node:fs/promises";
import http from "node:http";
import https from "node:https";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.dirname(fileURLToPath(import.meta.url));
const outputDir = path.join(rootDir, "dist");
const port = Number(process.env.PREVIEW_PORT || 9001);
const apiUpstream = new URL(process.env.ADMIN_API_UPSTREAM || "http://localhost:8910");
const apiBasePath = normalizeApiBasePath(
  process.env.ADMIN_API_BASE_PATH || "/api",
  "ADMIN_API_BASE_PATH",
);
const bootstrapPath = normalizeRuntimePath(
  process.env.ADMIN_BOOTSTRAP_PATH || "/admin/menu-resources",
  "ADMIN_BOOTSTRAP_PATH",
);
const contentTypes = new Map([
  [".css", "text/css; charset=utf-8"],
  [".html", "text/html; charset=utf-8"],
  [".ico", "image/x-icon"],
  [".js", "text/javascript; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
  [".png", "image/png"],
  [".svg", "image/svg+xml"],
  [".woff", "font/woff"],
  [".woff2", "font/woff2"],
]);

if (
  !/^https?:$/.test(apiUpstream.protocol) ||
  apiUpstream.pathname !== "/" ||
  apiUpstream.username ||
  apiUpstream.password ||
  apiUpstream.search ||
  apiUpstream.hash
) {
  throw new Error("ADMIN_API_UPSTREAM 必须是只包含 Origin 的 HTTP/HTTPS 地址");
}

function normalizeRuntimePath(value, name) {
  const normalized = value.trim();
  if (
    !/^\/[A-Za-z0-9._~/-]*$/.test(normalized) ||
    normalized === "/" ||
    normalized.startsWith("//") ||
    normalized.endsWith("/")
  ) {
    throw new Error(`${name} 必须是非根路径、无尾部斜杠的同源路径`);
  }
  return normalized;
}

function normalizeApiBasePath(value, name) {
  const normalized = value.trim();
  if (!/^\/[A-Za-z][A-Za-z0-9_-]*$/.test(normalized)) {
    throw new Error(`${name} 必须是单个安全路径段`);
  }
  if (normalized === "/admin" || normalized === "/healthz") {
    throw new Error(`${name} 与前端保留路径冲突`);
  }
  return normalized;
}

function applySecurityHeaders(response) {
  response.setHeader(
    "Content-Security-Policy",
    "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:; connect-src 'self'; worker-src 'self' blob:",
  );
  response.setHeader("X-Content-Type-Options", "nosniff");
  response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
  response.setHeader(
    "Permissions-Policy",
    "camera=(), microphone=(), geolocation=(), payment=(), usb=()",
  );
  response.setHeader("X-Frame-Options", "DENY");
}

function proxyApi(request, response, requestUrl) {
  const target = new URL(apiUpstream);
  target.pathname = `/api${requestUrl.pathname.slice(apiBasePath.length)}`;
  target.search = requestUrl.search;
  const transport = target.protocol === "https:" ? https : http;
  const proxyRequest = transport.request(
    target,
    {
      method: request.method,
      headers: {
        ...request.headers,
        host: target.host,
        "x-forwarded-host": request.headers.host || "localhost",
        "x-forwarded-proto": "http",
      },
    },
    (proxyResponse) => {
      response.writeHead(proxyResponse.statusCode || 502, proxyResponse.headers);
      proxyResponse.pipe(response);
    },
  );
  proxyRequest.on("error", (error) => {
    response.writeHead(502, { "Content-Type": "text/plain; charset=utf-8" });
    response.end(`API proxy error: ${error.message}`);
  });
  request.pipe(proxyRequest);
}

async function resolveStaticFile(pathname) {
  const decodedPath = decodeURIComponent(pathname);
  const absolutePath = path.resolve(outputDir, `.${decodedPath}`);
  if (absolutePath !== outputDir && !absolutePath.startsWith(`${outputDir}${path.sep}`)) {
    return null;
  }

  const candidates = [absolutePath, path.join(absolutePath, "index.html"), `${absolutePath}.html`];
  for (const candidate of candidates) {
    try {
      const fileStat = await stat(candidate);
      if (fileStat.isFile()) return candidate;
    } catch {
      // Try the next static-export path form.
    }
  }
  return null;
}

const server = http.createServer(async (request, response) => {
  const requestUrl = new URL(request.url || "/", `http://${request.headers.host || "localhost"}`);
  if (requestUrl.pathname.startsWith(`${apiBasePath}/`)) {
    proxyApi(request, response, requestUrl);
    return;
  }

  applySecurityHeaders(response);
  if (requestUrl.pathname === "/healthz") {
    response.writeHead(204);
    response.end();
    return;
  }
  if (requestUrl.pathname === "/") {
    response.writeHead(302, { Location: "/admin/" });
    response.end();
    return;
  }
  if (requestUrl.pathname === "/runtime-config.json") {
    response.writeHead(200, {
      "Cache-Control": "no-store, no-cache, must-revalidate",
      "Content-Type": "application/json; charset=utf-8",
    });
    response.end(JSON.stringify({ apiBaseUrl: apiBasePath, bootstrapPath }));
    return;
  }

  try {
    const filePath = await resolveStaticFile(requestUrl.pathname);
    if (!filePath) {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Not Found");
      return;
    }

    const extension = path.extname(filePath).toLowerCase();
    response.setHeader("Content-Type", contentTypes.get(extension) || "application/octet-stream");
    response.setHeader(
      "Cache-Control",
      requestUrl.pathname === "/runtime-config.json"
        ? "no-store, no-cache, must-revalidate"
        : requestUrl.pathname.startsWith("/_next/static/")
          ? "public, max-age=31536000, immutable"
          : "no-cache",
    );
    createReadStream(filePath).pipe(response);
  } catch (error) {
    response.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
    response.end(error instanceof Error ? error.message : "Static preview failed");
  }
});

server.listen(port, "0.0.0.0", () => {
  process.stdout.write(`Breezy Admin preview: http://localhost:${port}\n`);
  process.stdout.write(`API upstream: ${apiUpstream.origin}\n`);
  process.stdout.write(`API base path: ${apiBasePath}\n`);
});
