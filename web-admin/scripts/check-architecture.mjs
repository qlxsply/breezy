import fs from "node:fs";
import path from "node:path";
import process from "node:process";

import ts from "typescript";

const projectRoot = path.resolve(import.meta.dirname, "..");
const sourceRoot = path.join(projectRoot, "src");
const generatedDirectories = new Set([
  ".git",
  ".next",
  "coverage",
  "dist",
  "node_modules",
  "playwright-report",
  "test-results",
]);
const sourceExtensions = new Set([".ts", ".tsx"]);
const allowedSourceRoots = new Set([
  "app",
  "assets",
  "features",
  "runtime",
  "shared",
  "styles",
  "test",
]);
const legacySourceRoots = new Set(["api", "components", "core", "types"]);
const crossFeatureAllowlist = [
  /^features\/[^/]+\/public(?:\/|$)/,
  /^features\/resources\/permissions(?:\.ts)?$/,
];
const ordinaryFetchOwner = "shared/transport/http.ts";
const legacyPaths = [
  /^src\/style\.css$/,
  /^src\/app\/admin-shell\.css$/,
  /^src\/styles\/(?:admin(?:\/|$)|admin-page\.css$|common\.css$|list-page\.css$)/,
  /^src\/shared\/ui\/bz\/(?:bz|loading)\.css$/,
  /^src\/shared\/ui\/admin\/(?:admin-entity|inputs\/inputs)\.css$/,
];
const legacyImportPattern = /(?:@admin|src)\/(?:api|components|core|types)(?:\/|$)/;
const styleResidues = [
  { name: "Element 选择器", pattern: /\.el-/ },
  { name: "Element 变量", pattern: /--el-/ },
  { name: "旧 #app 根选择器", pattern: /#app\b/ },
  { name: "非必要 !important", pattern: /!important\b/ },
];

const violations = [];

function relative(file) {
  return path.relative(projectRoot, file).replaceAll(path.sep, "/");
}

function sourceRelative(file) {
  return path.relative(sourceRoot, file).replaceAll(path.sep, "/");
}

function walk(directory) {
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (entry.isDirectory() && generatedDirectories.has(entry.name)) continue;
    const target = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...walk(target));
    else if (entry.isFile()) files.push(target);
  }
  return files;
}

function report(rule, file, detail) {
  violations.push({ rule, file: relative(file), detail });
}

function print(line) {
  process.stdout.write(`${line}\n`);
}

function resolveSourceImport(importer, specifier) {
  if (specifier.startsWith("@admin/")) {
    return path.posix.normalize(specifier.slice("@admin/".length));
  }
  if (!specifier.startsWith(".")) return null;
  const resolved = path.resolve(path.dirname(importer), specifier);
  const insideSource = path.relative(sourceRoot, resolved);
  if (insideSource.startsWith("..") || path.isAbsolute(insideSource)) return null;
  return insideSource.replaceAll(path.sep, "/");
}

function layerOf(sourcePath) {
  return sourcePath.split("/", 1)[0];
}

function featureOf(sourcePath) {
  const match = /^features\/([^/]+)(?:\/|$)/.exec(sourcePath);
  return match?.[1] ?? null;
}

function importSpecifiers(sourceFile) {
  const imports = [];
  const visit = (node) => {
    if (
      (ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) &&
      node.moduleSpecifier &&
      ts.isStringLiteral(node.moduleSpecifier)
    ) {
      imports.push(node.moduleSpecifier.text);
    } else if (
      ts.isCallExpression(node) &&
      (node.expression.kind === ts.SyntaxKind.ImportKeyword ||
        (ts.isIdentifier(node.expression) && node.expression.text === "require")) &&
      node.arguments.length === 1 &&
      ts.isStringLiteral(node.arguments[0])
    ) {
      imports.push(node.arguments[0].text);
    }
    ts.forEachChild(node, visit);
  };
  visit(sourceFile);
  return imports;
}

function checkDependency(importer, specifier) {
  const imported = resolveSourceImport(importer, specifier);
  if (!imported) return;
  if (imported === ".." || imported.startsWith("../")) {
    report("非法 import", importer, `${specifier} 解析后超出 src`);
    return;
  }

  const from = sourceRelative(importer);
  const fromLayer = layerOf(from);
  const toLayer = layerOf(imported);
  const forbiddenByLayer =
    (fromLayer === "shared" && ["app", "features", "runtime"].includes(toLayer)) ||
    (fromLayer === "features" && ["app", "runtime"].includes(toLayer)) ||
    (fromLayer === "runtime" && toLayer === "app");
  if (forbiddenByLayer) {
    report("层级依赖", importer, `${fromLayer} 不得依赖 ${toLayer}: ${specifier}`);
  }

  const fromFeature = featureOf(from);
  const toFeature = featureOf(imported);
  if (
    fromFeature &&
    toFeature &&
    fromFeature !== toFeature &&
    !crossFeatureAllowlist.some((pattern) => pattern.test(imported))
  ) {
    report("跨 feature 依赖", importer, `${fromFeature} 不得直接依赖 ${imported}`);
  }

  if (legacyImportPattern.test(specifier)) {
    report("旧路径 import", importer, specifier);
  }
}

if (!fs.existsSync(sourceRoot)) {
  console.error(`架构检查失败: 找不到 ${sourceRoot}`);
  process.exit(1);
}

const allFiles = walk(sourceRoot);
const sourceFiles = allFiles.filter((file) => sourceExtensions.has(path.extname(file)));
const cssFiles = allFiles.filter((file) => path.extname(file) === ".css");
const declaredLiteralClasses = new Set();

for (const file of cssFiles) {
  const text = fs.readFileSync(file, "utf8");
  if (file.endsWith(".module.css")) {
    for (const globalMatch of text.matchAll(/:global\(([\s\S]*?)\)/g)) {
      for (const classMatch of globalMatch[1].matchAll(/\.([A-Za-z_][\w-]*)/g)) {
        declaredLiteralClasses.add(classMatch[1]);
      }
    }
  } else {
    for (const match of text.matchAll(/\.([A-Za-z_][\w-]*)/g)) {
      declaredLiteralClasses.add(match[1]);
    }
  }
}

for (const entry of fs.readdirSync(sourceRoot, { withFileTypes: true })) {
  if (!entry.isDirectory()) {
    report("最终目录", path.join(sourceRoot, entry.name), "src 根目录不得放置文件");
  } else if (!allowedSourceRoots.has(entry.name)) {
    const directory = path.join(sourceRoot, entry.name);
    const containsFiles = walk(directory).length > 0;
    if (containsFiles || !legacySourceRoots.has(entry.name)) {
      report("最终目录", directory, `不允许的 src 顶层目录: ${entry.name}`);
    }
  }
}

for (const file of allFiles) {
  const filePath = relative(file);
  if (legacyPaths.some((pattern) => pattern.test(filePath))) {
    report("旧路径/目录残留", file, filePath);
  }
}

for (const file of sourceFiles) {
  const text = fs.readFileSync(file, "utf8");
  if (/@ts-(?:ignore|nocheck|expect-error)\b/.test(text)) {
    report("TypeScript 绕过", file, "禁止使用 TypeScript 错误抑制指令");
  }
  const kind = path.extname(file) === ".tsx" ? ts.ScriptKind.TSX : ts.ScriptKind.TS;
  const sourceFile = ts.createSourceFile(file, text, ts.ScriptTarget.Latest, true, kind);
  for (const specifier of importSpecifiers(sourceFile)) checkDependency(file, specifier);
  const visit = (node) => {
    if (node.kind === ts.SyntaxKind.AnyKeyword) {
      const position = sourceFile.getLineAndCharacterOfPosition(node.getStart(sourceFile));
      report("显式 any", file, `第 ${position.line + 1} 行禁止显式 any`);
    }
    if (
      ts.isCallExpression(node) &&
      ts.isIdentifier(node.expression) &&
      node.expression.text === "fetch" &&
      sourceRelative(file) !== ordinaryFetchOwner
    ) {
      const position = sourceFile.getLineAndCharacterOfPosition(node.getStart(sourceFile));
      report("普通 fetch 归属", file, `第 ${position.line + 1} 行应通过 shared/transport`);
    }
    if (
      !file.endsWith(".test.tsx") &&
      ts.isJsxAttribute(node) &&
      node.name.text === "className" &&
      node.initializer
    ) {
      for (const className of jsxLiteralClassNames(node.initializer)) {
        if (!declaredLiteralClasses.has(className)) {
          const position = sourceFile.getLineAndCharacterOfPosition(node.getStart(sourceFile));
          report(
            "未定义全局样式",
            file,
            `第 ${position.line + 1} 行 className ${className} 没有对应的全局样式声明`,
          );
        }
      }
    }
    ts.forEachChild(node, visit);
  };
  visit(sourceFile);
}

function jsxLiteralClassNames(initializer) {
  if (ts.isStringLiteral(initializer)) return classNamesFromText(initializer.text);
  if (!ts.isJsxExpression(initializer) || !initializer.expression) return [];
  const expression = initializer.expression;
  if (ts.isStringLiteral(expression) || ts.isNoSubstitutionTemplateLiteral(expression)) {
    return classNamesFromText(expression.text);
  }
  if (ts.isTemplateExpression(expression)) {
    return [
      ...classNamesFromText(expression.head.text),
      ...expression.templateSpans.flatMap((span) => classNamesFromText(span.literal.text)),
    ];
  }
  if (ts.isArrayLiteralExpression(expression)) {
    return expression.elements.flatMap((element) =>
      ts.isStringLiteral(element) || ts.isNoSubstitutionTemplateLiteral(element)
        ? classNamesFromText(element.text)
        : [],
    );
  }
  return [];
}

function classNamesFromText(text) {
  return [...text.matchAll(/[A-Za-z_][\w-]*/g)].map((match) => match[0]);
}

for (const file of cssFiles) {
  const filePath = sourceRelative(file);
  if (
    !["app/globals.css", "styles/base.css", "styles/theme.css"].includes(filePath) &&
    !file.endsWith(".module.css")
  ) {
    report("样式归属", file, "组件或 feature 样式必须使用 *.module.css");
  }
  const text = fs.readFileSync(file, "utf8");
  for (const residue of styleResidues) {
    if (residue.pattern.test(text)) report("旧样式残留", file, residue.name);
  }
}

print("Web Admin 架构检查");
print(`- TypeScript: ${sourceFiles.length} 个文件`);
print(`- CSS: ${cssFiles.length} 个文件`);
print("- 生成目录: 已排除");

if (violations.length > 0) {
  console.error(`\n发现 ${violations.length} 项违规:`);
  for (const violation of violations) {
    console.error(`- [${violation.rule}] ${violation.file}: ${violation.detail}`);
  }
  process.exit(1);
}

print("结果: 通过");
