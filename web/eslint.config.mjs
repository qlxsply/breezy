import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier";
import importPlugin from "eslint-plugin-import";
import promisePlugin from "eslint-plugin-promise";
import simpleImportSort from "eslint-plugin-simple-import-sort";
import unusedImports from "eslint-plugin-unused-imports";
import pluginVue from "eslint-plugin-vue";
import globals from "globals";
import tseslint from "typescript-eslint";
import vueParser from "vue-eslint-parser";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default [
  {
    ignores: [
      "dist/**",
      "coverage/**",
      "node_modules/**",
      "public/mock/**",
      "public/**",
      "eslint.config.mjs",
      "prettier.config.mjs",
      "*.min.js",
      "*.min.css",
    ],
  },
  {
    files: ["**/*.{js,mjs,cjs,ts,mts,jsx,tsx,vue}"],
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
  ...pluginVue.configs["flat/recommended"],
  {
    files: ["**/*.vue"],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tseslint.parser,
        extraFileExtensions: [".vue"],
        ecmaVersion: "latest",
        sourceType: "module",
        tsconfigRootDir: __dirname,
      },
    },
  },
  {
    files: ["**/*.{js,mjs,cjs,ts,mts,jsx,tsx,vue}"],
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

      "vue/multi-word-component-names": "off",
      "vue/valid-template-root": "off",
      "vue/no-mutating-props": "error",
      "vue/component-tags-order": ["warn", { order: ["template", "script", "style"] }],
      "vue/block-order": ["warn", { order: ["template", "script", "style"] }],
      "vue/html-self-closing": [
        "warn",
        {
          html: { void: "always", normal: "never", component: "always" },
          svg: "always",
          math: "always",
        },
      ],
      "vue/max-attributes-per-line": "off",
      "vue/singleline-html-element-content-newline": "off",
      "vue/multiline-html-element-content-newline": "off",
    },
  },
  eslintConfigPrettier,
];
