import {defineConfig} from 'vitest/config';

export default defineConfig({
  resolve: {
    alias: {
      jquery: 'jquery/slim',
      '@': new URL('./src', import.meta.url).pathname,
    },
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.ts'],
    passWithNoTests: true,
    coverage: { provider: 'v8', reporter: ['text', 'html'] },
  },
});
