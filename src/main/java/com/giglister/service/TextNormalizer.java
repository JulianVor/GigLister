package com.giglister.service;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Normalizes names for robust duplicate matching (lowercase, no diacritics/punctuation). */
public final class TextNormalizer {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private TextNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return NON_ALNUM.matcher(decomposed.toLowerCase()).replaceAll("").trim();
    }

    /** Levenshtein distance between two normalized strings. */
    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    /** Similarity in [0,1], 1 = identical, based on normalized Levenshtein distance. */
    public static double similarity(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.isEmpty() && nb.isEmpty()) {
            return 1.0;
        }
        int maxLen = Math.max(na.length(), nb.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int distance = levenshtein(na, nb);
        return 1.0 - ((double) distance / maxLen);
    }
}
