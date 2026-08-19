import { defineConfig } from 'vite';

export default defineConfig({
  resolve: {
    alias: {
      // Runtime = build slim (mesmo que web/vendor usa hoje); os tipos vêm de @types/jquery,
      // que descreve o pacote 'jquery'. Todo código importa de 'jquery', nunca de 'jquery/slim'.
      jquery: 'jquery/slim',
      // Espelha o "@/*" -> "src/*" de tsconfig.json (paths do TS não resolvem sozinhos em
      // build/dev — só no type-check). `URL` global em vez de `node:url` pra não puxar
      // @types/node (o projeto não tem essa dependência).
      '@': new URL('./src', import.meta.url).pathname,
    },
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
