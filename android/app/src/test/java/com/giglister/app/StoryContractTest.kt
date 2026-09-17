package com.giglister.app

import com.giglister.app.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class StoryContractTest {
    @Test fun webLayersRetainAllTransformsAndIgnoreBrokenEntries() {
        val raw = """[{"id":"a","text":"Live!","centerXPct":25,"centerYPct":75,"scale":1.8,"rotationDeg":-24,"colorPos":73,"hasBox":true},{"id":"broken"}]"""
        val layer = parseStoryLayers(raw).single()
        assertEquals(25f, layer.centerXPct); assertEquals(-24f, layer.rotationDeg)
        assertEquals(73f, layer.colorPos); assertTrue(layer.hasBox)
        val encoded = StoryInput(imageUrl = "/photo.jpg").withLayers(listOf(layer))
        assertEquals(layer, parseStoryLayers(encoded.textLayersJson).single())
        assertEquals("Live!", encoded.text)
    }
    @Test fun tagsKeepSnapshotAndSeparateFromText() {
        val tag = StoryLayer(bandId = 62, bandName = "Hey Nille", profileImageUrl = "/avatar.jpg", logoUrl = "/logo.png", hasBox = true)
        val input = StoryInput().withLayers(listOf(StoryLayer(text = "  "), tag, StoryLayer(text = "a".repeat(300))))
        assertEquals(280, input.text!!.length)
        assertEquals(300, parseStoryLayers(input.textLayersJson).single().text.length)
        assertEquals(tag, parseStoryLayers(input.bandTagsJson, true).single())
        assertNull(StoryInput().withLayers(emptyList()).textLayersJson)
    }
    @Test fun oldAndMalformedLayersDoNotCrashViewer() {
        assertTrue(parseStoryLayers("not JSON").isEmpty())
        assertTrue(parseStoryLayers("{}").isEmpty())
        val old = parseStoryLayers("""[{"id":"old","text":"old","centerXPct":50,"centerYPct":50,"scale":1,"rotationDeg":0}]""").single()
        assertNull(old.colorPos); assertFalse(old.hasBox)
        assertTrue(parseStoryLayers("""[{"id":"bad","text":"x","centerXPct":50,"centerYPct":50,"scale":-1,"rotationDeg":0}]""").isEmpty())
    }
    @Test fun paletteMatchesWebWhiteRainbowBlack() {
        assertEquals(0xffffffff.toInt(), storyColor(null))
        assertEquals(0xffff0000.toInt(), storyColor(100f / 7))
        assertEquals(0xff00ffff.toInt(), storyColor(400f / 7))
        assertEquals(0xff000000.toInt(), storyColor(100f))
        assertEquals(storyColor(100f), storyColor(150f))
    }
    @Test fun photoFitPreservesAspectRatioInsideNineBySixteen() {
        for (aspect in listOf(.3f, 9f / 16f, 1f, 2f)) {
            val fit = storyFit(aspect)
            assertEquals(aspect, (fit.first * 9) / (fit.second * 16), .0001f)
            assertEquals(100f, maxOf(fit.first, fit.second), .0001f)
        }
    }
    @Test fun expirationBoundaryIsStrictAndProfileImageSurvivesEditPayload() {
        val now = Instant.parse("2026-09-17T10:00:00Z")
        assertFalse(BandStory(1, "image", createdAt = "", expiresAt = now.toString()).active(now))
        assertTrue(BandStory(1, "image", createdAt = "", expiresAt = now.plusSeconds(1).toString()).active(now))
        val input = BandInput("Band", null, null, null, null, "logo", "title", emptyList(), "portrait")
        assertEquals("portrait", Json.parseToJsonElement(storyJson.encodeToString(input)).jsonObject["profileImageUrl"]!!.jsonPrimitive.content)
    }
}
