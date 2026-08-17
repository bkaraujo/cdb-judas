import { defineConfig } from 'vitest/config';

export default defineConfig({
  resolve: { alias: { jquery: 'jquery/slim' } },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.ts'],
    passWithNoTests: true,
    coverage: { provider: 'v8', reporter: ['text', 'html'] },
  },
});
