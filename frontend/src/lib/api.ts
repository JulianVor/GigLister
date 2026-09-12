import "server-only";
import type {
  AdminBandListItem,
  AdminDashboardResponse,
  AdminEventListItem,
  AdminLocationListItem,
  AdminUserResponse,
  AuthResponse,
  BandImageDisplay,
  BandResponse,
  CalendarDayCount,
  ClaimResponse,
  DiscoverResponse,
  DuplicateCandidate,
  DuplicatePair,
  EntityMerge,
  EntityRef,
  EntityType,
  EventResponse,
  EventStatus,
  EventSummary,
  EntityStatus,
  LocationListItem,
  LocationResponse,
  MeResponse,
  MessageResponse,
  Page,
  PermissionLevel,
  PermissionResponse,
  RegisterResponse,
  SearchResults,
  SubmissionResponse,
  SubmissionStatus,
  UsernameAvailabilityResponse,
} from "./types";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

function toQuery(params: Record<string, string | number | boolean | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  }
  const qs = search.toString();
  return qs ? `?${qs}` : "";
}

async function apiFetch<T>(
  path: string,
  options: {
    method?: string;
    token?: string;
    body?: unknown;
    cache?: RequestCache;
  } = {}
): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers: {
      "Content-Type": "application/json",
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    cache: options.cache ?? "no-store",
  });

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!res.ok) {
    const message = data?.message ?? `Request to ${path} failed with ${res.status}`;
    throw new ApiError(res.status, message);
  }

  return data as T;
}

// --- Uploads ---

/**
 * Images are always uploaded, never entered as an external URL - this posts
 * the file as multipart/form-data (unlike apiFetch, which always sends JSON)
 * and hands back a URL that's already absolute and browser-reachable, so it
 * can be stored as-is into a logoUrl/titleImageUrl field.
 */
export async function uploadImage(file: File, token: string): Promise<{ url: string }> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch(`${API_BASE_URL}/api/uploads`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });

  const text = await res.text();
  const data = text ? JSON.parse(text) : undefined;
  if (!res.ok) {
    const message = data?.message ?? `Upload failed with ${res.status}`;
    throw new ApiError(res.status, message);
  }
  return data as { url: string };
}

// --- Auth ---

export function register(data: { email: string; password: string; username: string }) {
  return apiFetch<RegisterResponse>("/api/auth/register", { method: "POST", body: data });
}

export function usernameAvailable(username: string) {
  return apiFetch<UsernameAvailabilityResponse>(`/api/auth/username-available${toQuery({ username })}`);
}

export function verifyEmail(token: string) {
  return apiFetch<AuthResponse>("/api/auth/verify-email", { method: "POST", body: { token } });
}

export function forgotPassword(email: string) {
  return apiFetch<MessageResponse>("/api/auth/forgot-password", { method: "POST", body: { email } });
}

export function resetPassword(token: string, newPassword: string) {
  return apiFetch<AuthResponse>("/api/auth/reset-password", { method: "POST", body: { token, newPassword } });
}

export function login(data: { username: string; password: string }) {
  return apiFetch<AuthResponse>("/api/auth/login", { method: "POST", body: data });
}

// --- Events ---

export function getEvents(
  params: { city?: string; lat?: number; lon?: number; radiusKm?: number; from?: string; to?: string; page?: number; size?: number },
  token?: string
) {
  return apiFetch<Page<EventResponse>>(`/api/events${toQuery(params)}`, { token });
}

export function getEvent(id: number, token?: string) {
  return apiFetch<EventResponse>(`/api/events/${id}`, { token });
}

export function getCalendar(
  params: { year: number; month: number; city?: string; lat?: number; lon?: number; radiusKm?: number }
) {
  return apiFetch<CalendarDayCount[]>(`/api/events/calendar${toQuery(params)}`);
}

export interface EventInput {
  title?: string;
  date: string;
  startTime?: string;
  location: EntityRef;
  bands: EntityRef[];
  description?: string;
  ticketUrl?: string;
  titleImageUrl?: string;
  bandImageDisplay?: BandImageDisplay;
}

export function createEvent(data: EventInput, token: string) {
  return apiFetch<EventResponse>("/api/events", { method: "POST", body: data, token });
}

export function updateEvent(id: number, data: EventInput, token: string) {
  return apiFetch<EventResponse>(`/api/events/${id}`, { method: "PUT", body: data, token });
}

export function updateEventStatus(id: number, status: EventStatus, token: string) {
  return apiFetch<EventResponse>(`/api/events/${id}/status`, { method: "PATCH", body: { status }, token });
}

export function saveEvent(id: number, token: string) {
  return apiFetch<void>(`/api/events/${id}/save`, { method: "POST", token });
}

export function unsaveEvent(id: number, token: string) {
  return apiFetch<void>(`/api/events/${id}/save`, { method: "DELETE", token });
}

// --- Bands ---

export function getBands(params: { city?: string; page?: number; size?: number }) {
  return apiFetch<Page<BandResponse>>(`/api/bands${toQuery(params)}`);
}

export function getBand(id: number, token?: string) {
  return apiFetch<BandResponse>(`/api/bands/${id}`, { token });
}

export function getBandDuplicates(name: string, city?: string) {
  return apiFetch<DuplicateCandidate[]>(`/api/bands/duplicates${toQuery({ name, city })}`);
}

export interface BandInput {
  name: string;
  city?: string;
  country?: string;
  shortDescription?: string;
  website?: string;
  logoUrl?: string;
  titleImageUrl?: string;
  genres?: string[];
}

export function createBand(data: BandInput, token: string) {
  return apiFetch<BandResponse>("/api/bands", { method: "POST", body: data, token });
}

export function updateBand(id: number, data: BandInput, token: string) {
  return apiFetch<BandResponse>(`/api/bands/${id}`, { method: "PUT", body: data, token });
}

export function updateBandStatus(id: number, status: EntityStatus, token: string) {
  return apiFetch<BandResponse>(`/api/bands/${id}/status`, { method: "PATCH", body: { status }, token });
}

export function getBandPermissions(id: number, token: string) {
  return apiFetch<PermissionResponse[]>(`/api/bands/${id}/permissions`, { token });
}

export function grantBandPermission(id: number, userId: number, permission: PermissionLevel, token: string) {
  return apiFetch<void>(`/api/bands/${id}/permissions`, { method: "POST", body: { userId, permission }, token });
}

export function revokeBandPermission(id: number, userId: number, token: string) {
  return apiFetch<void>(`/api/bands/${id}/permissions/${userId}`, { method: "DELETE", token });
}

export function claimBand(id: number, message: string | undefined, token: string) {
  return apiFetch<ClaimResponse>(`/api/bands/${id}/claim`, { method: "POST", body: { message }, token });
}

export function followBand(id: number, token: string) {
  return apiFetch<void>(`/api/bands/${id}/follow`, { method: "POST", token });
}

export function unfollowBand(id: number, token: string) {
  return apiFetch<void>(`/api/bands/${id}/follow`, { method: "DELETE", token });
}

// --- Locations ---

export function getLocations(params: { city?: string; lat?: number; lon?: number; radiusKm?: number; page?: number; size?: number }) {
  return apiFetch<Page<LocationListItem>>(`/api/locations${toQuery(params)}`);
}

export function getLocation(id: number, token?: string) {
  return apiFetch<LocationResponse>(`/api/locations/${id}`, { token });
}

export function getLocationDuplicates(name: string, city?: string) {
  return apiFetch<DuplicateCandidate[]>(`/api/locations/duplicates${toQuery({ name, city })}`);
}

export interface LocationInput {
  name: string;
  city: string;
  address?: string;
  postalCode?: string;
  country?: string;
  website?: string;
  logoUrl?: string;
  titleImageUrl?: string;
  latitude?: number;
  longitude?: number;
}

export function createLocation(data: LocationInput, token: string) {
  return apiFetch<LocationResponse>("/api/locations", { method: "POST", body: data, token });
}

export function updateLocation(id: number, data: LocationInput, token: string) {
  return apiFetch<LocationResponse>(`/api/locations/${id}`, { method: "PUT", body: data, token });
}

export function updateLocationStatus(id: number, status: EntityStatus, token: string) {
  return apiFetch<LocationResponse>(`/api/locations/${id}/status`, { method: "PATCH", body: { status }, token });
}

export function getLocationPermissions(id: number, token: string) {
  return apiFetch<PermissionResponse[]>(`/api/locations/${id}/permissions`, { token });
}

export function grantLocationPermission(id: number, userId: number, permission: PermissionLevel, token: string) {
  return apiFetch<void>(`/api/locations/${id}/permissions`, { method: "POST", body: { userId, permission }, token });
}

export function revokeLocationPermission(id: number, userId: number, token: string) {
  return apiFetch<void>(`/api/locations/${id}/permissions/${userId}`, { method: "DELETE", token });
}

export function claimLocation(id: number, message: string | undefined, token: string) {
  return apiFetch<ClaimResponse>(`/api/locations/${id}/claim`, { method: "POST", body: { message }, token });
}

// --- Search / Discover ---

export function search(q: string, type?: string) {
  return apiFetch<SearchResults>(`/api/search${toQuery({ q, type })}`);
}

export function discover(params: { city?: string; lat?: number; lon?: number; radiusKm?: number }) {
  return apiFetch<DiscoverResponse>(`/api/discover${toQuery(params)}`);
}

// --- Me ---

export function getMe(token: string) {
  return apiFetch<MeResponse>("/api/me", { token });
}

export function updateProfile(
  data: { username?: string; homeCity?: string; homeLatitude?: number; homeLongitude?: number; radiusKm?: number },
  token: string
) {
  return apiFetch<MeResponse>("/api/me", { method: "PUT", body: data, token });
}

export function changePassword(data: { currentPassword: string; newPassword: string }, token: string) {
  return apiFetch<void>("/api/me/password", { method: "PUT", body: data, token });
}

/** "Meine Bands": every band the current user holds EDIT/MANAGE on. */
export function getMyBands(token: string) {
  return apiFetch<BandResponse[]>("/api/me/bands", { token });
}

/** "Meine Veranstaltungen": upcoming events across all of the user's bands, band-übergreifend. */
export function getMyEvents(token: string) {
  return apiFetch<EventSummary[]>("/api/me/events", { token });
}

// --- Admin ---

export function getAdminDashboard(token: string) {
  return apiFetch<AdminDashboardResponse>("/api/admin/dashboard", { token });
}

export function getAdminDuplicates(token: string) {
  return apiFetch<DuplicatePair[]>("/api/admin/duplicates", { token });
}

export function getAdminClaims(token: string) {
  return apiFetch<ClaimResponse[]>("/api/admin/claims", { token });
}

export function approveClaim(id: number, token: string) {
  return apiFetch<ClaimResponse>(`/api/admin/claims/${id}/approve`, { method: "POST", token });
}

export function rejectClaim(id: number, token: string) {
  return apiFetch<ClaimResponse>(`/api/admin/claims/${id}/reject`, { method: "POST", token });
}

export function mergeEntities(data: { entityType: EntityType; sourceEntityId: number; targetEntityId: number }, token: string) {
  return apiFetch<EntityMerge>("/api/admin/merge", { method: "POST", body: data, token });
}

export function getAdminUsers(query: string | undefined, token: string) {
  return apiFetch<AdminUserResponse[]>(`/api/admin/users${toQuery({ q: query })}`, { token });
}

export function promoteUser(id: number, token: string) {
  return apiFetch<AdminUserResponse>(`/api/admin/users/${id}/promote`, { method: "POST", token });
}

export function demoteUser(id: number, token: string) {
  return apiFetch<AdminUserResponse>(`/api/admin/users/${id}/demote`, { method: "POST", token });
}

/** One-off catch-up for locations saved before geocoding existed - see
 * LocationService.backfillMissingCoordinates on the backend. Can take a while (it
 * paces its own requests to Nominatim), so callers should show a loading state. */
export function geocodeMissingLocations(token: string) {
  return apiFetch<{ attempted: number; resolved: number }>("/api/admin/locations/geocode-missing", {
    method: "POST",
    token,
  });
}

export function getAdminBands(
  params: { status?: EntityStatus; q?: string; page?: number; size?: number },
  token: string
) {
  return apiFetch<Page<AdminBandListItem>>(`/api/admin/bands${toQuery(params)}`, { token });
}

export function getAdminLocations(
  params: { status?: EntityStatus; q?: string; page?: number; size?: number },
  token: string
) {
  return apiFetch<Page<AdminLocationListItem>>(`/api/admin/locations${toQuery(params)}`, { token });
}

export function getAdminEvents(
  params: { status?: EventStatus; q?: string; page?: number; size?: number },
  token: string
) {
  return apiFetch<Page<AdminEventListItem>>(`/api/admin/events${toQuery(params)}`, { token });
}

// --- GPT-skill submission review queue ---

export function getAdminSubmissions(status: SubmissionStatus | undefined, token: string) {
  return apiFetch<SubmissionResponse[]>(`/api/admin/submissions${toQuery({ status })}`, { token });
}

export function updateSubmission(
  id: number,
  data: { payload: Record<string, unknown>; imageUrl?: string },
  token: string
) {
  return apiFetch<SubmissionResponse>(`/api/admin/submissions/${id}`, { method: "PUT", body: data, token });
}

export function approveSubmission(id: number, token: string) {
  return apiFetch<SubmissionResponse>(`/api/admin/submissions/${id}/approve`, { method: "POST", token });
}

export function rejectSubmission(id: number, reason: string | undefined, token: string) {
  return apiFetch<SubmissionResponse>(`/api/admin/submissions/${id}/reject`, {
    method: "POST",
    body: { reason },
    token,
  });
}
