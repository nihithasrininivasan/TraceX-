import { defineConfig } from "vite";

export default defineConfig({
  server: { host: "localhost", port: 5173, strictPort: true },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("@xyflow") || id.includes("d3-")) return "network";
          if (id.includes("node_modules")) return "vendor";
        },
      },
    },
  },
});
