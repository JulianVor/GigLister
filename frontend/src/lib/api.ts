import "server-only";
import type {
  AdminBandListItem,
  AdminCreateUserResponse,
  AdminDashboardResponse,
  AdminEventListItem,
  AdminEventSeriesListItem,
  AdminLocationListItem,
  AdminUserResponse,
  AuthResponse,
  BandImageDisplay,
  BandResponse,
  BandStory,
  CalendarDayCount,
  ClaimResponse,
  DiscoverResponse,
  DuplicateCandidate,
  DuplicatePair,
  EntityMerge,
  EntityRef,
  EntityType,
  EventCreateResult,
  EventResponse,
  EventSeriesResponse,
  EventSeriesSummary,
  EventStatus,
  EventSummary,
  EntityStatus,
  GenreFilterOption,
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
  TimetableStyle,
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
  params: {
    city?: string;
    lat?: number;
    lon?: number;
    radiusKm?: number;
    from?: string;
    to?: string;
    genre?: string;
    page?: number;
    size?: number;
  },
  token?: string
) {
  return apiFetch<Page<EventResponse>>(`/api/events${toQuery(params)}`, { token });
}

/** Only ever returns genres with at least one matching upcoming event under the same
 * city/radius/date filters, so the filter bar never offers an option with zero results. */
export function getGenreFilters(params: {
  city?: string;
  lat?: number;
  lon?: number;
  radiusKm?: number;
  from?: string;
  to?: string;
}) {
  return apiFetch<GenreFilterOption[]>(`/api/events/genres${toQuery(params)}`);
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
  eventSeriesId?: number;
}

/** Publishes immediately (result.event set) for a platform admin or anyone with EDIT+ on
 * the location or a referenced band - otherwise the proposal is routed into the review
 * queue instead (result.submission set) and only goes live once a platform admin
 * approves it - see EventCreateResult. */
export function createEvent(data: EventInput, token: string) {
  return apiFetch<EventCreateResult>("/api/events", { method: "POST", body: data, token });
}

export function updateEvent(id: number, data: EventInput, token: string) {
  return apiFetch<EventResponse>(`/api/events/${id}`, { method: "PUT", body: data, token });
}

export function updateEventStatus(id: number, status: EventStatus, token: string) {
  return apiFetch<EventResponse>(`/api/events/${id}/status`, { method: "PATCH", body: { status }, token });
}

export function deleteEvent(id: number, token: string) {
  return apiFetch<void>(`/api/events/${id}`, { method: "DELETE", token });
}

export function saveEvent(id: number, token: string) {
  return apiFetch<void>(`/api/events/${id}/save`, { method: "POST", token });
}

export function unsaveEvent(id: number, token: string) {
  return apiFetch<void>(`/api/events/${id}/save`, { method: "DELETE", token });
}

export function saveAct(eventId: number, bandId: number, token: string) {
  return apiFetch<void>(`/api/events/${eventId}/bands/${bandId}/save`, { method: "POST", token });
}

export function unsaveAct(eventId: number, bandId: number, token: string) {
  return apiFetch<void>(`/api/events/${eventId}/bands/${bandId}/save`, { method: "DELETE", token });
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
  profileImageUrl?: string;
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

/** Refused by the backend (409) while any event still lists this band. */
export function deleteBand(id: number, token: string) {
  return apiFetch<void>(`/api/bands/${id}`, { method: "DELETE", token });
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

export function getBandStories(id: number, token?: string) {
  return apiFetch<BandStory[]>(`/api/bands/${id}/stories`, { token });
}

export interface BandStoryInput {
  imageUrl: string;
  text?: string;
  imgWidthPct: number;
  imgHeightPct: number;
  imgCenterXPct: number;
  imgCenterYPct: number;
  imgRotationDeg: number;
  imgBackgroundColor: string;
  textLayersJson?: string;
}

export function createBandStory(id: number, data: BandStoryInput, token: string) {
  return apiFetch<BandStory>(`/api/bands/${id}/stories`, { method: "POST", body: data, token });
}

export function deleteBandStory(id: number, storyId: number, token: string) {
  return apiFetch<void>(`/api/bands/${id}/stories/${storyId}`, { method: "DELETE", token });
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

/** Refused by the backend (409) while any event still lists this location. */
export function deleteLocation(id: number, token: string) {
  return apiFetch<void>(`/api/locations/${id}`, { method: "DELETE", token });
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

// --- Event series (Festivals) ---

/** Every series, for the "welches Festival?" dropdown on the event form and for a simple
 * admin overview - see EventSeries' own backend class comment for why there's no
 * published/draft split to filter by here. */
export function getEventSeriesList() {
  return apiFetch<EventSeriesSummary[]>("/api/event-series");
}

export function getEventSeries(id: number) {
  return apiFetch<EventSeriesResponse>(`/api/event-series/${id}`);
}

export interface EventSeriesInput {
  name: string;
  description?: string;
  titleImageUrl?: string;
  ticketUrl?: string;
  timetableStyle?: TimetableStyle;
}

export function createEventSeries(data: EventSeriesInput, token: string) {
  return apiFetch<EventSeriesResponse>("/api/event-series", { method: "POST", body: data, token });
}

export function updateEventSeries(id: number, data: EventSeriesInput, token: string) {
  return apiFetch<EventSeriesResponse>(`/api/event-series/${id}`, { method: "PUT", body: data, token });
}

// --- Search / Discover ---

/** token is optional and, passed, lets a logged-in user's search also surface their own
 * not-yet-published bands/locations (see SearchController/SearchService on the backend) -
 * same STUB/DRAFT/ARCHIVED-visible-to-any-logged-in-user rule the band/location detail
 * pages already apply, just reached via search instead of a direct link. */
export function search(q: string, type?: string, token?: string) {
  return apiFetch<SearchResults>(`/api/search${toQuery({ q, type })}`, { token });
}

export function discover(params: { city?: string; lat?: number; lon?: number; radiusKm?: number }, token?: string) {
  return apiFetch<DiscoverResponse>(`/api/discover${toQuery(params)}`, { token });
}

/** The full canonical base-genre list (see GenreTaxonomy) for the profile's genre picker -
 * unlike getGenreFilters this isn't narrowed to genres with an upcoming event. */
export function getGenres() {
  return apiFetch<string[]>("/api/genres");
}

// --- Me ---

export function getMe(token: string) {
  return apiFetch<MeResponse>("/api/me", { token });
}

export function updateProfile(
  data: {
    username?: string;
    homeCity?: string;
    homeLatitude?: number;
    homeLongitude?: number;
    radiusKm?: number;
    preferredGenres?: string[];
  },
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

/** "Meine Vorschläge": events this user proposed via createEvent without direct create
 * rights, awaiting (or already decided by) a platform admin. */
export function getMySubmissions(token: string) {
  return apiFetch<SubmissionResponse[]>("/api/me/submissions", { token });
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

/** "Kein Duplikat" - the pair stays as two separate entities, just stops being suggested
 * again (unlike mergeEntities, which folds one into the other). See AdminService.rejectDuplicate. */
export function rejectDuplicate(data: { entityType: EntityType; firstId: number; secondId: number }, token: string) {
  return apiFetch<void>("/api/admin/duplicates/reject", { method: "POST", body: data, token });
}

export function getAdminUsers(query: string | undefined, token: string) {
  return apiFetch<AdminUserResponse[]>(`/api/admin/users${toQuery({ q: query })}`, { token });
}

/** "Details ansehen" - same MeResponse shape a user gets about themselves under Mein
 * GigLister (gemerkte Konzerte, gefolgte Bands, ...), just for the admin looking at
 * someone else's account. See AdminService.userDetail. */
export function getAdminUserDetail(id: number, token: string) {
  return apiFetch<MeResponse>(`/api/admin/users/${id}`, { token });
}

/** Raw Response, not parsed JSON like apiFetch - the /admin/data/export route handler
 * streams this straight through as a file download, body and Content-Disposition header
 * both untouched. See AdminController.exportData. */
export function exportAllData(token: string) {
  return fetch(`${API_BASE_URL}/api/admin/data/export`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
}

/** Also a raw Response - the /admin/data/import route handler already has the request
 * body as plain text (from the browser's own fetch), so this just forwards it exactly as
 * received rather than round-tripping it through JSON.parse/stringify. See
 * AdminController.importData. */
export function importAllData(rawBody: string, token: string) {
  return fetch(`${API_BASE_URL}/api/admin/data/import`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: rawBody,
  });
}

/** No password in the request - the backend generates a temporary one and hands it back
 * once, in the response, for the admin to pass along to the new user themselves. */
export function createAdminUser(data: { email: string; username: string }, token: string) {
  return apiFetch<AdminCreateUserResponse>("/api/admin/users", { method: "POST", body: data, token });
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
  params: { status?: EntityStatus; q?: string; sort?: string; page?: number; size?: number },
  token: string
) {
  return apiFetch<Page<AdminBandListItem>>(`/api/admin/bands${toQuery(params)}`, { token });
}

/** "Alle auf Vollständigkeit setzen" - sweeps every STUB/DRAFT band and publishes any that
 * already clear BandService's isComplete bar. See BandService.publishAllComplete. */
export function publishCompleteBands(token: string) {
  return apiFetch<{ checked: number; published: number }>("/api/admin/bands/publish-complete", {
    method: "POST",
    token,
  });
}

export function getAdminLocations(
  params: { status?: EntityStatus; q?: string; sort?: string; page?: number; size?: number },
  token: string
) {
  return apiFetch<Page<AdminLocationListItem>>(`/api/admin/locations${toQuery(params)}`, { token });
}

/** Same idea as publishCompleteBands, for locations - see LocationService.publishAllComplete. */
export function publishCompleteLocations(token: string) {
  return apiFetch<{ checked: number; published: number }>("/api/admin/locations/publish-complete", {
    method: "POST",
    token,
  });
}

export function getAdminEventSeries(params: { q?: string; page?: number; size?: number }, token: string) {
  return apiFetch<Page<AdminEventSeriesListItem>>(`/api/admin/festivals${toQuery(params)}`, { token });
}

export function getAdminEvents(
  params: { status?: EventStatus; q?: string; sort?: string; page?: number; size?: number },
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
