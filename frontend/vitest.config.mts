import { defineConfig } from 'vitest/config';

// Vite 8 handles JSX (tsconfig "react-jsx") and the "@/*" alias natively,
// so no @vitejs/plugin-react or vite-tsconfig-paths.
export default defineConfig({
  resolve: {
    tsconfigPaths: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    env: {
      BACKEND_URL: 'http://backend.test',
    },
  },
});
