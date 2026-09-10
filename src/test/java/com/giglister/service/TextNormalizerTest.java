package com.giglister.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    void normalizesDiacriticsCaseAndPunctuation() {
        assertThat(TextNormalizer.normalize("Hafenklang!")).isEqualTo("hafenklang");
        assertThat(TextNormalizer.normalize("Hafenklang Hamburg")).isEqualTo("hafenklanghamburg");
        assertThat(TextNormalizer.normalize("Mötley Crüe")).isEqualTo("motleycrue");
    }

    @Test
    void similarityIsHighForNearDuplicatesAndLowForUnrelatedNames() {
        assertThat(TextNormalizer.similarity("Hafenklang", "Hafenklang")).isEqualTo(1.0);
        assertThat(TextNormalizer.similarity("Hafenklang", "Hafen Klang")).isGreaterThan(0.85);
        assertThat(TextNormalizer.similarity("Hafenklang", "Molotow")).isLessThan(0.4);
    }
}
