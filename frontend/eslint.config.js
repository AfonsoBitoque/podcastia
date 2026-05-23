import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import prettierConfig from 'eslint-config-prettier'
import { defineConfig, globalIgnores } from 'eslint/config'
import vitestPlugin from 'eslint-plugin-vitest'

export default defineConfig([
  globalIgnores(['dist', '.vite', 'node_modules', 'playwright-report', 'test-results']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
      prettierConfig,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: {
        ...globals.browser,
        process: 'readonly',
      },
      parserOptions: {
        ecmaVersion: 'latest',
        ecmaFeatures: { jsx: true },
        sourceType: 'module',
      },
    },
    rules: {
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]' }],
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
  },
  // Configuração específica para testes (Vitest)
  {
    files: ['src/**/*.test.{js,jsx}', 'src/**/*.spec.{js,jsx}'],
    plugins: {
      vitest: vitestPlugin,
    },
    languageOptions: {
      globals: {
        ...vitestPlugin.environments.env.globals,
      },
    },
    rules: {
      ...vitestPlugin.configs.recommended.rules,
      'vitest/expect-expect': 'off'
    },
  },
  // Configuração específica para E2E (Playwright)
  {
    files: ['e2e/**/*.{js,jsx}', '**/*.spec.{js,jsx}'],
    languageOptions: {
      globals: {
        process: 'readonly',
      },
    },
  },
  // Configuração para Service Worker
  {
    files: ['public/sw.js'],
    languageOptions: {
      globals: {
        ...globals.serviceworker,
      },
    },
  },
])