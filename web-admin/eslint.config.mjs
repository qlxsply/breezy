import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier";
import importPlugin from "eslint-plugin-import";
import promisePlugin from "eslint-plugin-promise";
import simpleImportSort from "eslint-plugin-simple-import-sort";
import unusedImports from "eslint-plugin-unused-imports";
import globals from "globals";
import tseslint from "typescript-eslint";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

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
      promise: promisePlugin,
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

      "promise/always-return": "off",
      "promise/catch-or-return": "off",

      "no-empty": ["warn", { allowEmptyCatch: true }],
      "no-misleading-character-class": "warn",
      "no-useless-escape": "warn",
    },
  },
  eslintConfigPrettier,
];
