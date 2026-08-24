import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { gzipSync } from "node:zlib";

const projectRoot = path.resolve(import.meta.dirname, "..");
const distRoot = path.join(projectRoot, "dist");
const budgetPath = path.join(projectRoot, "bundle-budget.json");

function walk(directory) {
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const target = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...walk(target));
    else if (entry.isFile()) files.push(target);
  }
  return files;
}

function formatBytes(bytes) {
  return `${(bytes / 1024).toFixed(1)} KiB`;
}

function print(line) {
  process.stdout.write(`${line}\n`);
}

function routeFromHtml(file) {
  const relative = path.relative(distRoot, file).replaceAll(path.sep, "/");
  if (relative === "index.html") return "/";
  return `/${relative.replace(/(?:\/index)?\.html$/, "")}`;
}

function assetsFromHtml(html) {
  const assets = new Set();
  for (const tag of html.match(/<(?:script|link)\b[^>]*>/gi) ?? []) {
    const asset = /\b(?:src|href)=["']([^"']+)["']/i.exec(tag)?.[1];
    if (!asset) continue;
    const pathname = asset.split(/[?#]/, 1)[0];
    if (/^\/_next\/static\/.+\.(?:js|css)$/.test(pathname)) assets.add(pathname);
  }
  return assets;
}

function measureAsset(asset) {
  const file = path.join(distRoot, ...decodeURIComponent(asset).split("/").filter(Boolean));
  if (!fs.existsSync(file)) throw new Error(`HTML 引用了不存在的静态资源: ${asset}`);
  const content = fs.readFileSync(file);
  return { asset, raw: content.length, gzip: gzipSync(content, { level: 9 }).length };
}

function exceeds(measurement, limit) {
  return measurement.raw > limit.raw || measurement.gzip > limit.gzip;
}

if (!fs.existsSync(distRoot) || !fs.existsSync(budgetPath)) {
  console.error("Bundle budget 检查失败: 请先执行生产构建，并确认 bundle-budget.json 存在");
  process.exit(1);
}

const budget = JSON.parse(fs.readFileSync(budgetPath, "utf8"));
const htmlFiles = walk(distRoot).filter((file) => file.endsWith(".html"));
const staticAssetFiles = walk(path.join(distRoot, "_next", "static")).filter((file) =>
  /\.(?:js|css)$/.test(file),
);
const routes = htmlFiles
  .map((file) => {
    const assets = [...assetsFromHtml(fs.readFileSync(file, "utf8"))];
    const measurements = assets.map(measureAsset);
    return {
      route: routeFromHtml(file),
      assets,
      measurements,
      raw: measurements.reduce((total, item) => total + item.raw, 0),
      gzip: measurements.reduce((total, item) => total + item.gzip, 0),
    };
  })
  .sort((left, right) => left.route.localeCompare(right.route));

const failures = [];
const buildMtime = Math.max(...htmlFiles.map((file) => fs.statSync(file).mtimeMs));
const sourceFiles = [
  ...walk(path.join(projectRoot, "src")),
  path.join(projectRoot, "next.config.ts"),
  path.join(projectRoot, "package.json"),
  path.join(projectRoot, "package-lock.json"),
];
const sourceMtime = Math.max(...sourceFiles.map((file) => fs.statSync(file).mtimeMs));
if (sourceMtime > buildMtime + 1_000) {
  failures.push("dist 早于当前源码或构建配置，请重新执行生产构建");
}
const routeGroups = [
  {
    name: "登录路由",
    routes: routes.filter(({ route }) => route === "/admin/login"),
    limit: budget.routes.login,
  },
  {
    name: "后台路由",
    routes: routes.filter(({ route }) => route.startsWith("/admin") && route !== "/admin/login"),
    limit: budget.routes.admin,
  },
];

print("Web Admin Bundle Budget");
for (const group of routeGroups) {
  print(
    `\n${group.name} (上限 raw ${formatBytes(group.limit.raw)} / gzip ${formatBytes(group.limit.gzip)})`,
  );
  if (group.routes.length === 0) failures.push(`${group.name}没有匹配到导出 HTML`);
  for (const route of group.routes) {
    const marker = exceeds(route, group.limit) ? "FAIL" : "PASS";
    print(
      `- ${marker} ${route.route.padEnd(41)} raw ${formatBytes(route.raw).padStart(10)} / gzip ${formatBytes(route.gzip).padStart(10)}`,
    );
    if (marker === "FAIL") failures.push(`${route.route} 超出${group.name}预算`);
  }
}

const allStaticAssets = new Map(
  staticAssetFiles.map((file) => {
    const asset = `/${path.relative(distRoot, file).replaceAll(path.sep, "/")}`;
    return [asset, measureAsset(asset)];
  }),
);
const chunkFailures = [];
for (const measurement of allStaticAssets.values()) {
  const type = path.extname(measurement.asset).slice(1);
  const limit = budget.singleChunk[type];
  if (limit && exceeds(measurement, limit)) chunkFailures.push({ ...measurement, limit });
}

print("\n单 chunk 预算");
for (const [type, limit] of Object.entries(budget.singleChunk)) {
  const matching = [...allStaticAssets.values()].filter((item) => item.asset.endsWith(`.${type}`));
  const largest = matching.sort((left, right) => right.raw - left.raw)[0];
  print(
    `- ${type.toUpperCase()}: 最大 ${largest ? `${formatBytes(largest.raw)} raw / ${formatBytes(largest.gzip)} gzip` : "无"}; 上限 ${formatBytes(limit.raw)} / ${formatBytes(limit.gzip)}`,
  );
}
for (const failure of chunkFailures) {
  failures.push(
    `${failure.asset} 超出单 ${path.extname(failure.asset).slice(1).toUpperCase()} chunk 预算`,
  );
}

const adminRoutes = routes.filter(({ route }) => route.startsWith("/admin"));
const pageChunkOwners = new Map();
for (const route of adminRoutes) {
  const pageChunks = route.assets.filter((asset) =>
    /\/_next\/static\/chunks\/app\/admin\/.+\/page-[^/]+\.js$/.test(asset),
  );
  if (pageChunks.length !== 1) {
    failures.push(`${route.route} 应恰好加载 1 个后台 page chunk，实际为 ${pageChunks.length}`);
  }
  for (const chunk of pageChunks) {
    const owners = pageChunkOwners.get(chunk) ?? [];
    owners.push(route.route);
    pageChunkOwners.set(chunk, owners);
  }
}
const sharedPageChunks = [...pageChunkOwners].filter(([, owners]) => owners.length > 1);
for (const [chunk, owners] of sharedPageChunks) {
  failures.push(`page chunk 跨路由复用: ${chunk} -> ${owners.join(", ")}`);
}
print(
  `\n路由隔离: ${adminRoutes.length} 个后台 HTML，${pageChunkOwners.size} 个 page chunk，${sharedPageChunks.length} 个跨路由复用`,
);

if (failures.length > 0) {
  console.error(`\n发现 ${failures.length} 项 bundle budget 违规:`);
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

print("\n结果: 通过");
