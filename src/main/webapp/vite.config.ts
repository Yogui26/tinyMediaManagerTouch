import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// The production build is written into the Java resources and served by tinyMediaManager itself (WebAppHandler).
// During development the API of a running tinyMediaManager instance is proxied.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../resources/webapp',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': process.env.TMM_API_URL ?? 'http://localhost:7878',
    },
  },
});
