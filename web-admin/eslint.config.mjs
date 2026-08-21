import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier";
import importPlugin from "eslint-plugin-import";
import simpleImportSort from "eslint-plugin-simple-import-sort";
import unusedImports from "eslint-plugin-unused-imports";
import globals from "globals";
import tseslint from "typescript-eslint";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const removedImportPattern = {
  group: [
    "@admin/components/bz",
    "@admin/components/bz/**",
    "@admin/components/admin",
    "@admin/components/admin/**",
    "@admin/components/admin-inputs",
    "@admin/components/admin-inputs/**",
    "@admin/core",
    "@admin/core/**",
    "@admin/api/http",
    "@admin/api/auth",
    "@admin/api/notifications",
    "@admin/api/push",
    "@admin/api/sse",
    "@admin/api/permissions",
    "@admin/components/runtime",
    "@admin/components/runtime/**",
    "@admin/types/notification",
    "@admin/types/permission-admin",
    "@admin/types/resource-admin",
  ],
  message: "旧公共路径已删除，请使用 canonical shared、feature 或 runtime 路径。",
};

export default [
  {
    ignores: [
      ".next/**",
      "dist/**",
      "node_modules/**",
      "eslint.config.mjs",
      "prettier.config.mjs",
      "*.min.js",
      "*.min.css",
    ],
  },
  {
    files: ["**/*.{js,mjs,cjs,ts,mts,jsx,tsx}"],
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ["**/*.{js,mjs,cjs,ts,mts,jsx,tsx}"],
    plugins: {
      import: importPlugin,
      "simple-import-sort": simpleImportSort,
      "unused-imports": unusedImports,
    },
    rules: {
      "no-alert": "warn",
      "no-console": ["warn", { allow: ["warn", "error"] }],
      "no-debugger": "warn",
      "no-var": "error",
      "prefer-const": "warn",
      eqeqeq: ["error", "always", { null: "ignore" }],
      curly: ["error", "all"],
      "object-shorthand": ["warn", "always"],

      "@typescript-eslint/consistent-type-imports": [
        "warn",
        { prefer: "type-imports", fixStyle: "inline-type-imports" },
      ],
      "@typescript-eslint/no-empty-object-type": "warn",
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/no-unused-vars": "off",

      "import/first": "error",
      "import/newline-after-import": ["warn", { count: 1 }],
      "import/no-duplicates": "warn",
      "simple-import-sort/imports": "warn",
      "simple-import-sort/exports": "warn",
      "unused-imports/no-unused-imports": "error",
      "unused-imports/no-unused-vars": [
        "warn",
        {
          args: "after-used",
          argsIgnorePattern: "^_",
          varsIgnorePattern: "^_",
          caughtErrors: "all",
          caughtErrorsIgnorePattern: "^_",
        },
      ],

      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: ["../../**"],
              message: "跨目录依赖必须使用 @admin 路径别名。",
            },
            removedImportPattern,
          ],
        },
      ],

      "no-empty": ["warn", { allowEmptyCatch: true }],
      "no-misleading-character-class": "warn",
      "no-useless-escape": "warn",
    },
  },
  {
    files: ["src/features/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            removedImportPattern,
            {
              group: ["../../**"],
              message: "feature 内跨目录依赖必须使用 @admin 路径别名。",
            },
            {
              group: [
                "@admin/runtime",
                "@admin/runtime/**",
                "@admin/app",
                "@admin/app/**",
                "@admin/api",
                "@admin/api/**",
                "@admin/components",
                "@admin/components/**",
                "@admin/core",
                "@admin/core/**",
                "@admin/types",
                "@admin/types/**",
              ],
              message: "feature 不得反向依赖 runtime、应用组件或旧 core。",
            },
          ],
        },
      ],
      "no-restricted-syntax": [
        "error",
        {
          selector: "NewExpression[callee.name='EventSource']",
          message: "EventSource 只能由 runtime adapter 创建。",
        },
        {
          selector: "NewExpression[callee.name='BroadcastChannel']",
          message: "BroadcastChannel 只能由 runtime adapter 创建。",
        },
        {
          selector: "MemberExpression[object.name='navigator'][property.name='serviceWorker']",
          message: "Service Worker 只能由 runtime adapter 管理。",
        },
        {
          selector:
            "MemberExpression[object.name='Notification'][property.name='requestPermission']",
          message: "Notification 权限只能由 runtime adapter 请求。",
        },
      ],
    },
  },
  {
    files: ["src/features/**/model/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            removedImportPattern,
            {
              group: ["../../**", "../api", "../api/**", "../service", "../service/**"],
              message: "model 内跨目录依赖必须使用 @admin 路径别名。",
            },
            {
              group: [
                "@admin/runtime",
                "@admin/runtime/**",
                "@admin/app",
                "@admin/app/**",
                "@admin/api",
                "@admin/api/**",
                "@admin/components",
                "@admin/components/**",
                "@admin/core",
                "@admin/core/**",
                "@admin/types",
                "@admin/types/**",
                "@admin/shared/transport",
                "@admin/shared/transport/**",
                "@admin/features/*/api",
                "@admin/features/*/api/**",
                "@admin/features/*/service",
                "@admin/features/*/service/**",
              ],
              message: "model/store 只能依赖模型类型与 shared 底层能力。",
            },
          ],
        },
      ],
    },
  },
  {
    files: ["src/shared/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            removedImportPattern,
            {
              group: ["../../**"],
              message: "shared 内跨目录依赖必须使用 @admin/shared 路径别名。",
            },
            {
              group: [
                "@admin/api",
                "@admin/api/**",
                "@admin/app",
                "@admin/app/**",
                "@admin/components",
                "@admin/components/**",
                "@admin/core",
                "@admin/core/**",
                "@admin/features",
                "@admin/features/**",
                "@admin/runtime",
                "@admin/runtime/**",
                "@admin/types",
                "@admin/types/**",
              ],
              message: "shared 层不得反向依赖应用、业务 API、registry 或业务类型。",
            },
          ],
        },
      ],
    },
  },
  {
    files: ["src/shared/ui/bz/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            removedImportPattern,
            {
              group: [
                "../../**",
                "../admin",
                "../admin/**",
                "@admin/shared/ui/admin",
                "@admin/shared/ui/admin/**",
              ],
              message: "Bz UI 只能依赖自身及 shared 底层能力。",
            },
            {
              group: [
                "@admin/api",
                "@admin/api/**",
                "@admin/app",
                "@admin/app/**",
                "@admin/components",
                "@admin/components/**",
                "@admin/core",
                "@admin/core/**",
                "@admin/features",
                "@admin/features/**",
                "@admin/runtime",
                "@admin/runtime/**",
                "@admin/types",
                "@admin/types/**",
              ],
              message: "Bz UI 不得依赖认证、registry、业务 API 或业务类型。",
            },
          ],
        },
      ],
    },
  },
  eslintConfigPrettier,
];
