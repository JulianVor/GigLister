// Cookie name shared between the server-only reader (the festival detail page) and the
// client-side FestivalEventsFilter toggle, which writes it directly via document.cookie -
// same split as location-cookies.ts.
export const FESTIVAL_EVENTS_FILTER_COOKIE = "giglister_festival_events_filter";

export type FestivalEventsFilterValue = "ALL" | "SAVED";
