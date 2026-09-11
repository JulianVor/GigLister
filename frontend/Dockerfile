FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# git doesn't track empty directories, so a public/ with nothing in it yet
# (as in this repo) wouldn't exist after a fresh clone - guarantee it here
# so the runner stage's COPY below never fails looking for it.
RUN mkdir -p public

# Baked into the client bundle at build time (the browser calls this directly,
# never through the Docker network) - override at build time for a non-localhost setup.
ARG NEXT_PUBLIC_API_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

# Same GIGLISTER_UPLOAD_MAX_SIZE_MB the backend enforces (see .env.example) - the
# client-side size check in ImageUploadField needs it baked in the same way, since it
# runs in the browser.
ARG NEXT_PUBLIC_UPLOAD_MAX_SIZE_MB=5
ENV NEXT_PUBLIC_UPLOAD_MAX_SIZE_MB=$NEXT_PUBLIC_UPLOAD_MAX_SIZE_MB

# next.config.ts's own read of this (for the Server Action body limit) isn't
# browser code, so it doesn't need the NEXT_PUBLIC_ prefix - but `next build`
# freezes the resolved config into the standalone output, so it still has to
# be set here at build time, not just at container runtime.
ARG GIGLISTER_UPLOAD_MAX_SIZE_MB=5
ENV GIGLISTER_UPLOAD_MAX_SIZE_MB=$GIGLISTER_UPLOAD_MAX_SIZE_MB

RUN npm run build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000
ENV PORT=3000
CMD ["node", "server.js"]
