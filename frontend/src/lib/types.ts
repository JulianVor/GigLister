// Mirrors the DTOs exposed by the Spring Boot backend (see backend `dto` package).

export type EntityStatus = "STUB" | "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type EventStatus = "DRAFT" | "PUBLISHED" | "CANCELLED";
export type EntityType = "BAND" | "LOCATION" | "EVENT_SERIES";
export type PermissionLevel = "EDIT" | "MANAGE";
export type ClaimStatus = "PENDING" | "APPROVED" | "REJECTED";
export type BandImageDisplay = "LOGO" | "PHOTO";
export type TimetableStyle = "LIST" | "GRID";

export interface GenreFilterOption {
  genre: string;
  eventCount: number;
}

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
  genres: string[];
  /** This band's own start time within the event it's listed under (HH:mm:ss), distinct
   * from the event's own overall startTime - null means it shares the event's time. */
  startTime: string | null;
}

export interface LocationSummary {
  id: number;
  name: string;
  city: string;
  status: EntityStatus;
  titleImageUrl: string | null;
  linkable: boolean;
  latitude: number | null;
  longitude: number | null;
}

export interface EventSeriesSummary {
  id: number;
  name: string;
  titleImageUrl: string | null;
  ticketUrl: string | null;
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
  eventSeries: EventSeriesSummary | null;
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
  eventSeries: EventSeriesSummary | null;
}

export interface EventSeriesResponse {
  id: number;
  name: string;
  description: string | null;
  titleImageUrl: string | null;
  ticketUrl: string | null;
  timetableStyle: TimetableStyle;
  events: EventSummary[];
}

export interface AdminEventSeriesListItem {
  id: number;
  name: string;
  eventCount: number;
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
  status: EntityStatus;
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
  profileImageUrl: string | null;
  genres: string[];
  status: EntityStatus;
  unclaimed: boolean;
  upcomingEvents: EventSummary[];
}

export interface BandStory {
  id: number;
  imageUrl: string;
  text: string | null;
  // How the band positioned/scaled/rotated imageUrl within the 9:16 story frame - see
  // CroppedStoryImage. Null for a story that somehow lacks crop data (falls back to
  // object-contain there).
  imgWidthPct: number | null;
  imgHeightPct: number | null;
  imgCenterXPct: number | null;
  imgCenterYPct: number | null;
  imgRotationDeg: number | null;
  imgBackgroundColor: string | null;
  createdAt: string;
  expiresAt: string;
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
  homeLatitude: number | null;
  homeLongitude: number | null;
  radiusKm: number | null;
  preferredGenres: string[];
  platformAdmin: boolean;
  mustChangePassword: boolean;
  savedEvents: EventSummary[];
  /** Individual festival acts (band-within-event) gemerkt - every one implies its eventId
   * is also in savedEvents (see UserService.saveAct), the reverse isn't true. */
  savedActs: { eventId: number; bandId: number }[];
  followedBands: {
    id: number;
    name: string;
    logoUrl: string | null;
    profileImageUrl: string | null;
    hasActiveStory: boolean;
    nextEventDate: string | null;
  }[];
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
  mustChangePassword: boolean;
}

export interface AdminCreateUserResponse {
  id: number;
  email: string;
  username: string;
  temporaryPassword: string;
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
  completeness: number;
}

export interface AdminLocationListItem {
  id: number;
  name: string;
  city: string | null;
  status: EntityStatus;
  completeness: number;
}

export interface AdminEventListItem {
  id: number;
  date: string;
  title: string | null;
  locationName: string;
  bandNames: string[];
  status: EventStatus;
  completeness: number;
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
  recommendedForYou: EventSummary[];
}

export interface EntityRef {
  id?: number;
  name?: string;
  city?: string;
  address?: string;
  postalCode?: string;
  /** Only meaningful when this ref is one of an event's `bands` - this band's own start
   * time within the show (HH:mm), distinct from the event's overall startTime. Ignored
   * for `location`. */
  startTime?: string;
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
  /** Set (BAND/LOCATION only) when this isn't a proposal for a new entity, but an
   * enrichment for this existing STUB/DRAFT one. */
  targetEntityId: number | null;
  /** Null for a GPT-skill proposal - set when a logged-in user submitted this themselves
   * (see EventInput/createEvent) because they had no direct create rights. */
  submittedBy: number | null;
  submittedByUsername: string | null;
}

/** POST /api/events's response: either it published immediately (event set) because the
 * caller has direct create rights, or it was routed into the review queue instead
 * (submission set) for a platform admin to approve or reject - see createEvent. */
export interface EventCreateResult {
  published: boolean;
  event: EventResponse | null;
  submission: SubmissionResponse | null;
}
