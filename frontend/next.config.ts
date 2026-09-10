import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Slim standalone server output (only the files needed at runtime) - what
  // the frontend Dockerfile's final stage copies, per Next.js's own Docker guide.
  output: "standalone",
};

export default nextConfig;
