import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Slim standalone server output (only the files needed at runtime) - what
  // the frontend Dockerfile's final stage copies, per Next.js's own Docker guide.
  output: "standalone",
  experimental: {
    serverActions: {
      // Server Actions default to a 1MB body limit, well under the backend's
      // 5MB upload limit (UploadService) - image uploads go through
      // uploadImageAction, so this has to be at least that large. Comfortable
      // headroom above 5MB (multipart framing adds a little, and this layer's
      // own "Body exceeded" error is a lot uglier than the backend's own
      // "Datei ist zu groß" - better a file has to clear both limits to be
      // rejected here instead of this one).
      bodySizeLimit: "8mb",
    },
  },
};

export default nextConfig;
