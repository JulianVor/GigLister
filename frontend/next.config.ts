import type { NextConfig } from "next";

// Same GIGLISTER_UPLOAD_MAX_SIZE_MB the backend uses (see .env.example) - read directly
// here (not NEXT_PUBLIC_-prefixed) since next.config.ts is never browser code. `next
// build` resolves and freezes this into the standalone output's server config, though,
// so - like NEXT_PUBLIC_ vars - it still has to be set at build time (see Dockerfile),
// not just as a runtime env var on the running container.
const uploadMaxSizeMb = Number(process.env.GIGLISTER_UPLOAD_MAX_SIZE_MB) || 5;

const nextConfig: NextConfig = {
  // Slim standalone server output (only the files needed at runtime) - what
  // the frontend Dockerfile's final stage copies, per Next.js's own Docker guide.
  output: "standalone",
  experimental: {
    serverActions: {
      // Server Actions default to a 1MB body limit. Image uploads go through
      // uploadImageAction, so this has to comfortably clear the real limit -
      // ImageUploadField already rejects an oversized file before it's ever
      // sent, so this is just a safety net (plus a little headroom for
      // multipart framing overhead); this layer's own "Body exceeded" error
      // is a lot uglier than the backend's "Datei ist zu groß".
      bodySizeLimit: `${uploadMaxSizeMb + 2}mb`,
    },
  },
};

export default nextConfig;
