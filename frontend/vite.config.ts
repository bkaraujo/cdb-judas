import { defineConfig } from 'vite';

export default defineConfig({
  resolve: {
    // Runtime = build slim (mesmo que web/vendor usa hoje); os tipos vêm de @types/jquery,
    // que descreve o pacote 'jquery'. Todo código importa de 'jquery', nunca de 'jquery/slim'.
    alias: { jquery: 'jquery/slim' },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/login': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  build: { outDir: 'dist', emptyOutDir: true },
});
