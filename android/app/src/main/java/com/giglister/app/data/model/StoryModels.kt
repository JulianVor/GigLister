package com.giglister.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt

@Serializable data class BandStory(
    val id: Long, val imageUrl: String, val text: String? = null,
    val imgWidthPct: Float? = null, val imgHeightPct: Float? = null,
    val imgCenterXPct: Float? = null, val imgCenterYPct: Float? = null,
    val imgRotationDeg: Float? = null, val imgBackgroundColor: String? = null,
    val textLayersJson: String? = null, val bandTagsJson: String? = null,
    val createdAt: String, val expiresAt: String
) {
    fun active(now: Instant = Instant.now()) = runCatching { Instant.parse(expiresAt).isAfter(now) }.getOrDefault(false)
}

/** Same percentage coordinates and opaque JSON payloads as the web composer. */
@Serializable data class StoryInput(
    val imageUrl: String = "", val text: String? = null,
    val imgWidthPct: Float = 100f, val imgHeightPct: Float = 100f,
    val imgCenterXPct: Float = 50f, val imgCenterYPct: Float = 50f,
    val imgRotationDeg: Float = 0f, val imgBackgroundColor: String = "#111111",
    val textLayersJson: String? = null, val bandTagsJson: String? = null
)
@Serializable data class StoryBandOption(val id: Long, val name: String, val profileImageUrl: String? = null, val logoUrl: String? = null)
@Serializable data class StoryLayer(
    val id: String = UUID.randomUUID().toString(), val text: String = "",
    val bandId: Long? = null, val bandName: String? = null,
    val profileImageUrl: String? = null, val logoUrl: String? = null,
    val centerXPct: Float = 50f, val centerYPct: Float = 50f,
    val scale: Float = 1f, val rotationDeg: Float = 0f,
    val colorPos: Float? = null, val hasBox: Boolean = false
)
val storyJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
fun parseStoryLayers(raw: String?, tags: Boolean = false): List<StoryLayer> = runCatching {
    (storyJson.parseToJsonElement(raw ?: "[]") as JsonArray).mapNotNull { element ->
        runCatching {
            val obj = element.jsonObject
            require(listOf("id", "centerXPct", "centerYPct", "scale", "rotationDeg").all { it in obj })
            val layer = storyJson.decodeFromJsonElement<StoryLayer>(element)
            require(listOf(layer.centerXPct, layer.centerYPct, layer.scale, layer.rotationDeg).all { it.isFinite() })
            require(layer.scale > 0 && (layer.colorPos == null || layer.colorPos.isFinite()))
            require(if (tags) layer.bandId != null && layer.bandName != null else "text" in obj)
            layer
        }.getOrNull()
    }
}.getOrDefault(emptyList())
fun StoryInput.withLayers(layers: List<StoryLayer>): StoryInput {
    val texts = layers.filter { it.bandId == null && it.text.isNotBlank() }
    val tags = layers.filter { it.bandId != null }
    return copy(text = texts.joinToString(" · ") { it.text.trim() }.take(280).ifEmpty { null },
        textLayersJson = texts.takeIf { it.isNotEmpty() }?.let { storyJson.encodeToString(it) },
        bandTagsJson = tags.takeIf { it.isNotEmpty() }?.let { storyJson.encodeToString(it) })
}
fun storyFit(aspect: Float): Pair<Float, Float> = if (aspect > 9f / 16f) 100f to (100f * 9f / 16f / aspect) else (100f * aspect * 16f / 9f) to 100f
fun storyColor(pos: Float?): Int {
    val colors = listOf(0xffffff, 0xff0000, 0xffff00, 0x00ff00, 0x00ffff, 0x0000ff, 0xff00ff, 0x000000)
    val scaled = ((pos ?: 0f).coerceIn(0f, 100f) / 100f) * 7
    val i = scaled.toInt().coerceAtMost(6)
    val fraction = scaled - i
    var result = 0xff000000.toInt()
    for (shift in listOf(16, 8, 0)) {
        val a = colors[i] shr shift and 255; val b = colors[i + 1] shr shift and 255
        result = result or ((a + (b - a) * fraction).roundToInt() shl shift)
    }
    return result
}
