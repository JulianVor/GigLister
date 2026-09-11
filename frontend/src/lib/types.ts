// Mirrors the DTOs exposed by the Spring Boot backend (see backend `dto` package).

export type EntityStatus = "STUB" | "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type EventStatus = "DRAFT" | "PUBLISHED" | "CANCELLED";
export type EntityType = "BAND" | "LOCATION";
export type PermissionLevel = "EDIT" | "MANAGE";
export type ClaimStatus = "PENDING" | "APPROVED" | "REJECTED";
export type BandImageDisplay = "LOGO" | "PHOTO";

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface BandSummary {
  id: number;
  name: string;
  city: string | null;
  status: EntityStatus;
  logoUrl: string | null;
  titleImageUrl: string | null;
  linkable: boolean;
}

export interface LocationSummary {
  id: number;
  name: string;
  city: string;
  status: EntityStatus;
  titleImageUrl: string | null;
  linkable: boolean;
}

export interface EventSummary {
  id: number;
  title: string | null;
  date: string; // ISO yyyy-MM-dd
  startTime: string | null; // HH:mm:ss
  location: LocationSummary;
  bands: BandSummary[];
  titleImageUrl: string | null;
  bandImageDisplay: BandImageDisplay;
  status: EventStatus;
}

export interface EventResponse {
  id: number;
  title: string | null;
  date: string;
  startTime: string | null;
  location: LocationSummary;
  bands: BandSummary[];
  description: string | null;
  ticketUrl: string | null;
  titleImageUrl: string | null;
  bandImageDisplay: BandImageDisplay;
  status: EventStatus;
  createdBy: number;
}

export interface LocationResponse {
  id: number;
  name: string;
  city: string;
  address: string | null;
  postalCode: string | null;
  country: string | null;
  website: string | null;
  logoUrl: string | null;
  titleImageUrl: string | null;
  latitude: number | null;
  longitude: number | null;
  status: EntityStatus;
  unclaimed: boolean;
  upcomingEvents: EventSummary[];
  pastEventsByYear: Record<string, EventSummary[]>;
}

export interface LocationListItem {
  id: number;
  name: string;
  city: string;
  upcomingEventCount: number;
}

export interface BandResponse {
  id: number;
  name: string;
  city: string | null;
  country: string | null;
  shortDescription: string | null;
  website: string | null;
  logoUrl: string | null;
  titleImageUrl: string | null;
  genres: string[];
  status: EntityStatus;
  unclaimed: boolean;
  upcomingEvents: EventSummary[];
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  username: string;
  platformAdmin: boolean;
}

export interface RegisterResponse {
  userId: number;
  email: string;
  message: string;
}

export interface UsernameAvailabilityResponse {
  available: boolean;
}

export interface MessageResponse {
  message: string;
}

export interface MeResponse {
  id: number;
  email: string;
  username: string;
  homeCity: string | null;
  radiusKm: number | null;
  platformAdmin: boolean;
  savedEvents: EventSummary[];
  followedBands: { id: number; name: string; nextEventDate: string | null }[];
  managedEntities: {
    entityType: EntityType;
    entityId: number;
    name: string;
    permission: PermissionLevel;
  }[];
}

export interface PermissionResponse {
  userId: number;
  email: string;
  username: string;
  permission: PermissionLevel;
}

export interface ClaimResponse {
  id: number;
  entityType: EntityType;
  entityId: number;
  entityName: string;
  requestedBy: number;
  requestedByEmail: string;
  message: string | null;
  status: ClaimStatus;
  requestedAt: string;
}

export interface AdminUserResponse {
  id: number;
  email: string;
  username: string;
  platformAdmin: boolean;
}

export interface DuplicateCandidate {
  entityType: EntityType;
  id: number;
  name: string;
  city: string | null;
  similarity: number;
}

/** A likely-duplicate pair surfaced to admins — both ids are needed to merge. */
export interface DuplicatePair {
  entityType: EntityType;
  firstId: number;
  firstName: string;
  secondId: number;
  secondName: string;
  city: string | null;
  similarity: number;
}

export interface AdminDashboardResponse {
  openClaims: number;
  bandsNeedingAttention: number;
  locationsNeedingAttention: number;
  possibleDuplicates: number;
  pendingSubmissions: number;
}

export interface AdminBandListItem {
  id: number;
  name: string;
  city: string | null;
  status: EntityStatus;
}

export interface AdminLocationListItem {
  id: number;
  name: string;
  city: string | null;
  status: EntityStatus;
}

export interface AdminEventListItem {
  id: number;
  date: string;
  title: string | null;
  locationName: string;
  bandNames: string[];
  status: EventStatus;
}

export interface CalendarDayCount {
  date: string;
  count: number;
}

export interface SearchResults {
  events: EventSummary[];
  bands: BandResponse[];
  locations: LocationListItem[];
}

export interface DiscoverResponse {
  todayNearby: EventSummary[];
  thisWeekend: EventSummary[];
  newEvents: EventSummary[];
  locationsWithUpcomingShows: LocationListItem[];
  bandsPlayingSoon: BandResponse[];
}

export interface EntityRef {
  id?: number;
  name?: string;
  city?: string;
  address?: string;
  postalCode?: string;
}

export interface EntityMerge {
  id: number;
  entityType: EntityType;
  sourceEntityId: number;
  targetEntityId: number;
  sourceNameAlias: string | null;
  mergedBy: number;
  mergedAt: string;
}

export interface ApiErrorBody {
  status: number;
  error: string;
  message: string;
}

export type SubmissionType = "BAND" | "LOCATION" | "EVENT";
export type SubmissionStatus = "PENDING" | "APPROVED" | "REJECTED";

/** From the GPT-skill review queue - payload is arbitrary JSON matching the create-request
 * shape for `type` (BandCreateRequest/LocationCreateRequest/EventCreateRequest). */
export interface SubmissionResponse {
  id: number;
  type: SubmissionType;
  payload: Record<string, unknown>;
  imageUrl: string | null;
  status: SubmissionStatus;
  submittedAt: string;
  reviewedBy: number | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  resultEntityId: number | null;
}
