import path from "path";
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      "/api/auth": { target: "http://localhost:8081", changeOrigin: true },
      "/market-data/api": {
        target: "http://localhost:8084",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/market-data/, ""),
      },
      "/api/v1/transactions": {
        target: "http://localhost:8082",
        changeOrigin: true,
      },
      "/portfolio-api": {
        target: "http://localhost:8083",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/portfolio-api/, ""),
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
