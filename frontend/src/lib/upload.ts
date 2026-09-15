// Same GIGLISTER_UPLOAD_MAX_SIZE_MB the backend enforces (see .env.example) - baked into
// the browser bundle at build time via NEXT_PUBLIC_UPLOAD_MAX_SIZE_MB (docker-compose.yml/
// Dockerfile). Checked client-side, before a file is ever sent, because a file this large
// blows past the Server Action's own body-size limit first - that crashes with a raw
// Next.js error page instead of the backend's friendly "Datei ist zu groß" message.
export const MAX_UPLOAD_SIZE_MB = Number(process.env.NEXT_PUBLIC_UPLOAD_MAX_SIZE_MB) || 5;
export const MAX_UPLOAD_SIZE_BYTES = MAX_UPLOAD_SIZE_MB * 1024 * 1024;
