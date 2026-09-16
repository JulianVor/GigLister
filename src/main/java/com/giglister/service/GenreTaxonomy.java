package com.giglister.service;

import java.util.List;

/**
 * Bands enter genres as free text ("Stonerpunk", "Alternative Rock", ...), so there's no
 * controlled vocabulary to build a filter UI from directly - one filter per literal genre
 * string would be an unusable wall of near-duplicates. Instead this is a curated list of
 * base genres; a filter matches any band whose genre text CONTAINS it as a substring
 * (case-insensitive), so "Stonerpunk" surfaces under both "Stoner" and "Punk" without either
 * needing to be its own literal genre anywhere. Mostly kept to plain, single words (no
 * "Post-Punk"/"Hip-Hop") so a compound like "Postpunk" or "Post-Punk" still matches "Punk" -
 * splitting further than that would need real NLP, not string matching. "Nu-Metal" and the
 * two "Progressive ..." entries are the deliberate exceptions: unlike the single-word
 * entries, they only match a band genre that spells out that exact phrase (hyphen/space
 * included), so a band tagged just "Progressive" or "Metal" alone won't surface under either.
 */
public final class GenreTaxonomy {

    public static final List<String> BASE_GENRES = List.of(
            "Alternative", "Ambient", "Black", "Blues", "Country", "Death", "Deathcore", "Doom",
            "Drone", "Dub", "Electro", "Emo", "Experimental", "Folk", "Funk", "Gothic",
            "Grindcore", "Grunge", "Hardcore", "House", "Indie", "Industrial", "Jazz", "Metal",
            "Metalcore", "Noise", "Nu-Metal", "Pop", "Progressive Metal", "Progressive Rock",
            "Psychedelic", "Punk", "Rap", "Reggae", "Rock", "Shoegaze", "Ska", "Soul", "Stoner",
            "Synth", "Techno", "Thrash", "Trance", "Wave", "World"
    );

    private GenreTaxonomy() {
    }

    /** Whether any of the band's free-text genres contains this base genre as a substring. */
    public static boolean matches(List<String> bandGenres, String baseGenre) {
        return bandGenres.stream().anyMatch(g -> g != null && g.toLowerCase().contains(baseGenre.toLowerCase()));
    }
}
